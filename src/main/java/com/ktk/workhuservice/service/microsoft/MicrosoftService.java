package com.ktk.workhuservice.service.microsoft;

import com.ktk.workhuservice.config.MicrosoftConfig;
import com.ktk.workhuservice.data.activity.Activity;
import com.ktk.workhuservice.data.activityitems.ActivityItemService;
import com.ktk.workhuservice.data.rounds.Round;
import com.ktk.workhuservice.data.users.User;
import com.ktk.workhuservice.enums.TransactionType;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody;
import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Service
public class MicrosoftService {

    private static ConfidentialClientApplication app;
    private final MicrosoftConfig config;
    private ActivityItemService activityItemService;

    public MicrosoftService(MicrosoftConfig config, ActivityItemService activityItemService) {
        this.config = config;
        this.activityItemService = activityItemService;
    }

    public void sendActivityToSharePointListItem(Activity activity) throws Exception {

        GraphServiceClient graphClient = getGraphClient();

        var sumHours = activityItemService.sumHoursByActivity(activity.getId());

        ListItem listItem = new ListItem();
        FieldValueSet fields = new FieldValueSet();
        HashMap<String, Object> additionalData = new HashMap<>();

        var items = activityItemService.findByActivity(activity.getId());
        String xlsx = MicrosoftUtils.createXlsxFromActivity(activity, items, sumHours, new ClassPathResource("imports/docs/munkalap_sablon_uj.xlsx").getInputStream());

        if (activity.getTransactionType().equals(TransactionType.HOURS)) {
            createPaidListItem(activity, additionalData, graphClient, sumHours);
            fields.setAdditionalData(additionalData);
            listItem.setFields(fields);
            sendXlsxToSharepointFolder(graphClient, activity, xlsx);
            graphClient.sites().bySiteId(config.getSiteId()).lists().byListId(config.getPaidJobsId()).items().post(listItem);
            sendMailToEmployer(graphClient, activity, sumHours, xlsx);
        } else if (Arrays.asList(TransactionType.DUKA_MUNKA, TransactionType.DUKA_MUNKA_2000).contains(activity.getTransactionType())) {
            createUnpaidListItem(activity, additionalData, graphClient, sumHours);
            fields.setAdditionalData(additionalData);
            listItem.setFields(fields);
            sendXlsxToSharepointFolder(graphClient, activity, xlsx);
            graphClient.sites().bySiteId(config.getSiteId()).lists().byListId(config.getUnpaidJobsId()).items().post(listItem);
        }

    }

    private void sendMailToEmployer(GraphServiceClient graphClient, Activity activity, double sumHours, String xlsx) throws Exception {

        FileAttachment attachment = new FileAttachment();
        attachment.setName(buildFilename(activity));
        attachment.setContentType("text/plain");
        attachment.setContentBytes(new FileInputStream(xlsx).readAllBytes());

        ArrayList<Attachment> attachmentList = new ArrayList<>();
        attachmentList.add(attachment);

        sendEmail(createContent(activity, sumHours), activity.getEmployer().getEmail(), String.format("Befizetés:  %s", activity.getDescription()), attachmentList);

    }

    private String createContent(Activity activity, double sumHours) {
        String date = MicrosoftUtils.formatDate(activity.getActivityDateTime().toLocalDate());
        return "Kedves " + activity.getEmployer().getFullName() + "!\n\n" + date +
                " dátummal egy munka került rögzítésre rendszerünkbe. A munka részleteit a csatolt munkalapon láthatod.\n\nAz elvégzett munka utáni befizetendő összeg: " + (int) (sumHours * 3000) +
                "Ft\n\nKérjük, a befizetendő összeget a MyShare számlára utald el!\nSzámlaszám: 10700323-43750203-52000001\nKözlemény: " + date.replace(".", "") + "_" + activity.getEmployer().getFullName()
                + "\n\nHa már utaltál, akkor tekintsd tárgytalannak az emailt. Kérdés esetén erre az email-re válaszolva veheted fel velünk a kapcsolatot. \n\nÜdvözlettel, \nMyShare csapat";

    }

    private Drive getDriveId(GraphServiceClient graphServiceClient) {
        return graphServiceClient.sites().bySiteId(config.getSiteId()).drive().get();
    }

    private void sendXlsxToSharepointFolder(GraphServiceClient graphServiceClient, Activity activity, String xlsx) throws IOException {
        Drive drive = getDriveId(graphServiceClient);
        ByteArrayInputStream input = new ByteArrayInputStream(FileUtils.readFileToByteArray(new File(xlsx)));
        graphServiceClient.drives().byDriveId(drive.getId()).root().content().withUrl(buildUri(drive.getId(), buildFilename(activity)))
                .put(input);
    }

    private String buildFilename(Activity activity) {
        return MicrosoftUtils.formatDate(activity.getActivityDateTime().toLocalDate()).replace(".", "") + "_" + MicrosoftUtils.changeSpecChars(activity.getDescription().trim()) + ".xlsx";
    }

    private String buildUri(String driveId, String filename) {
        return "https://graph.microsoft.com/v1.0/sites/" + config.getSiteId() +
                "/drives/" +
                driveId +
                "/root:/WORK/Munkalapok/" + filename + ":/content";
    }

    private void createUnpaidListItem(Activity activity, HashMap<String, Object> additionalData, GraphServiceClient graphClient, double sumHours) {
        var teamsLookUpId = getUserTeamsListId(graphClient, activity.getResponsible().getId());
        additionalData.put("Title", activity.getDescription());
        additionalData.put("ResponsibleLookupId", teamsLookUpId);
        additionalData.put("Activitydate", activity.getActivityDateTime().toString());
        additionalData.put("Hours", String.valueOf(sumHours));
        additionalData.put("Credits", String.valueOf(sumHours * (activity.getTransactionType().equals(TransactionType.DUKA_MUNKA_2000) ? 2000 : 1000)));
        additionalData.put("Appactivityid", String.valueOf(activity.getId()));
    }

    private void createPaidListItem(Activity activity, HashMap<String, Object> additionalData, GraphServiceClient graphClient, double sumHours) {
        var teamsLookUpId = getUserTeamsListId(graphClient, activity.getEmployer().getId());
        additionalData.put("Title", activity.getDescription());
        additionalData.put("EmployerLookupId", teamsLookUpId);
        additionalData.put("Activitydate", activity.getActivityDateTime().toString());
        additionalData.put("Hours", String.valueOf(sumHours));
        additionalData.put("MySharecredits", String.valueOf(sumHours * 3000));
        additionalData.put("WorkId", String.valueOf(activity.getId()));
    }

    private String getUserTeamsListId(GraphServiceClient graphClient, Long id) {
        var result = graphClient.sites().bySiteId(config.getSiteId()).lists().byListId(config.getWorkUsersId()).items().get(requestOptions -> {
            requestOptions.queryParameters.expand = new String[]{"fields"};
            requestOptions.queryParameters.filter = "fields/Title eq " + id;
            requestOptions.headers.add("Prefer", "HonorNonIndexedQueriesWarningMayFailRandomly");
        });

        return result.getValue().get(0).getFields().getId();
    }

    public void sendStatusUpdate(Integer currentCredit, double currentStatus, Integer creditToBeOnTrack, User user, Round round) throws Exception {

        String content = createStatusMailBody(user, currentCredit, currentStatus, creditToBeOnTrack, round);
        sendEmail(content, user.getEmail(), "Státusz update", new ArrayList<Attachment>());
    }

    public void sendNewPassword(User user, String newPassword) throws Exception {

        String content = createNewPasswordMailBody(user, newPassword);
        sendEmail(content, user.getEmail(), "Új jelszó", new ArrayList<Attachment>());
    }

    private void sendEmail(String content, String email, String subject, ArrayList<Attachment> attachmentList) throws Exception {
        GraphServiceClient graphClient = getGraphClient();

        Message message = new Message();
        ItemBody body = new ItemBody();
        body.setContentType(BodyType.Text);

        body.setContent(content);
        message.setBody(body);
        message.setSubject(subject);

        LinkedList<Recipient> toRecipientsList = new LinkedList<>();
        Recipient toRecipients = new Recipient();
        EmailAddress emailAddress1 = new EmailAddress();
        emailAddress1.setAddress(email);
        toRecipients.setEmailAddress(emailAddress1);
        toRecipientsList.add(toRecipients);
        message.setToRecipients(toRecipientsList);

        if (!attachmentList.isEmpty()) {
            message.setAttachments(attachmentList);
        }

        SendMailPostRequestBody request = new SendMailPostRequestBody();
        request.setMessage(message);
        request.setSaveToSentItems(false);

        graphClient.users().byUserId(config.getMyshareMail()).sendMail().post(request);
    }

    private String createStatusMailBody(User user, Integer currentCredit, double currentStatus, Integer creditToBeOnTrack, Round round) {
        return String.format("Kedves %s!\n\nJelenlegi MyShare státuszod: %s (%.2f%%).\nAz OnTrackhez szükséges összeg: %s.\nEzt %s %s-ig tudod befizetni.\n\nÜdvözlettel, \nMyShare csapat",
                user.getFullName(), currentCredit, currentStatus, creditToBeOnTrack, round.getEndDateTime().toLocalDate(), round.getEndDateTime().toLocalTime());

    }

    private String createNewPasswordMailBody(User user, String newPassword) {
        return String.format("Kedves %s!\n\n%s felhasználóhoz tartozó új jelszavad: %s\n\nBelépés itt: https://dukapp.bcc-ktk.org/login\n\nÜdvözlettel, \nMyShare csapat",
                user.getFullName(), user.getUsername(), newPassword);

    }

    private void buildConfidentialClientObject() throws Exception {
        app = ConfidentialClientApplication.builder(
                        config.getClientId(),
                        ClientCredentialFactory.createFromSecret(config.getClientSecret()))
                .authority(config.getRealm())
                .build();
    }

    private IAuthenticationResult getAccessTokenByClientCredentialGrant() throws Exception {
        ClientCredentialParameters clientCredentialParam = ClientCredentialParameters.builder(
                        Collections.singleton(config.getScope()))
                .build();

        return app.acquireToken(clientCredentialParam).get();
    }

    private GraphServiceClient getGraphClient() throws Exception {
        buildConfidentialClientObject();
        IAuthenticationResult accessTokenResult = getAccessTokenByClientCredentialGrant();

        var authProvider = new AuthenticationProvider() {
            @Override
            public void authenticateRequest(RequestInformation request, Map<String, Object> additionalAuthenticationContext) {
                request.headers.add("Authorization", "Bearer " + accessTokenResult.accessToken());
                additionalAuthenticationContext.put("Authorization", "Bearer " + accessTokenResult.accessToken());
            }
        };

        return new GraphServiceClient(authProvider);
    }

}
