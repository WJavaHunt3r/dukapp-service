package com.ktk.dukappservice.service.microsoft;

import com.ktk.dukappservice.config.MicrosoftConfig;
import com.ktk.dukappservice.data.activity.Activity;
import com.ktk.dukappservice.data.activityitems.ActivityItemService;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.TransactionType;
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

        var items = activityItemService.findByActivity(activity.getId()).toList();
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
        int totalAmount = (int) (sumHours * 3000);
        String notice = date.replace(".", "").trim() + "_" + activity.getEmployer().getFullName();

        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333; }
                .container { max-width: 600px; background-color: #ffffff; border-radius: 8px; margin: 0 auto; padding: 30px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }
                .header { border-bottom: 2px solid #eef2f5; padding-bottom: 15px; margin-bottom: 20px; }
                .header h2 { color: #0056b3; margin: 0; font-size: 22px; }
                .notice-box { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 18px; margin: 20px 0; }
                .amount-card { background-color: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 6px; padding: 15px; text-align: center; margin: 20px 0; color: #065f46; }
                .amount-title { font-size: 14px; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 5px; color: #047857; }
                .amount-value { font-size: 24px; font-weight: bold; }
                .info-table { width: 100%%; border-collapse: collapse; margin-top: 10px; }
                .info-table td { padding: 8px 0; border-bottom: 1px dashed #cbd5e1; font-size: 14px; }
                .info-table tr:last-child td { border-bottom: none; }
                .label { color: #64748b; font-weight: 500; }
                .value { font-weight: bold; color: #0f172a; text-align: right; }
                .copy-text { font-family: 'Courier New', Courier, monospace; background-color: #e2e8f0; padding: 2px 6px; border-radius: 4px; }
                .note { background-color: #fffbebf5; border-left: 4px solid #f59e0b; padding: 12px 15px; border-radius: 0 4px 4px 0; font-size: 13px; color: #78350f; margin: 20px 0; }
                .footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #eef2f5; font-size: 13px; color: #94a3b8; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2>Kedves %s!</h2>
                </div>
                
                <p>Értesítünk, hogy <strong>%s</strong> dátummal egy új munka került rögzítésre a rendszerünkben. A munka részletes leírását a csatolt munkalapon találod.</p>
                
                <div class="amount-card">
                    <div class="amount-title">Befizetendő összeg</div>
                    <div class="amount-value">%d Ft</div>
                </div>

                <div class="notice-box">
                    <p style="margin-top: 0; font-weight: bold; color: #1e293b;">Utalási adatok:</p>
                    <table class="info-table">
                        <tr>
                            <td class="label">Kedvezményezett:</td>
                            <td class="value">MyShare</td>
                        </tr>
                        <tr>
                            <td class="label">Számlaszám:</td>
                            <td class="value"><span class="copy-text">10700323-43750203-52000001</span></td>
                        </tr>
                        <tr>
                            <td class="label">Közlemény:</td>
                            <td class="value"><span class="copy-text">%s</span></td>
                        </tr>
                    </table>
                </div>

                <div class="note">
                    <strong>Megjegyzés:</strong> Ha az összeget már átutaltad, kérjük, tekintsd ezt az üzenetet tárgytalannak. Kérdés esetén válaszolj bátran erre az e-mailre.
                </div>

                <div class="footer">
                    Üdvözlettel,<br>
                    <strong>MyShare csapat</strong>
                </div>
            </div>
        </body>
        </html>
        """,
                activity.getEmployer().getFullName(),
                date,
                totalAmount,
                notice
        );
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

        String content = createPasswordResetMailBody(user, newPassword);
        sendEmail(content, user.getEmail(), "Új jelszó", new ArrayList<Attachment>());
    }

    private void sendEmail(String content, String email, String subject, ArrayList<Attachment> attachmentList) throws Exception {
        GraphServiceClient graphClient = getGraphClient();

        Message message = new Message();
        ItemBody body = new ItemBody();
        body.setContentType(BodyType.Html);

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

//    private String createStatusMailBody(User user, Integer currentCredit, double currentStatus, Integer creditToBeOnTrack, Round round) {
//        return String.format("Kedves %s!\n\nJelenlegi MyShare státuszod: %s (%.2f%%).\nAz OnTrackhez szükséges összeg: %s.\nEzt %s %s-ig tudod befizetni.\n\nÜdvözlettel, \nMyShare csapat",
//                user.getFullName(), currentCredit, currentStatus, creditToBeOnTrack, round.getEndDateTime().toLocalDate(), round.getEndDateTime().toLocalTime());
//
//    }

    private String createPasswordResetMailBody(User user, String newPassword) {
        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333; }
                .container { max-width: 600px; background-color: #ffffff; border-radius: 8px; margin: 0 auto; padding: 30px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }
                .header { border-bottom: 2px solid #eef2f5; padding-bottom: 15px; margin-bottom: 20px; }
                .header h2 { color: #0056b3; margin: 0; font-size: 22px; }
                .password-card { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 20px; text-align: center; margin: 25px 0; }
                .username-label { font-size: 13px; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
                .username-value { font-size: 16px; font-weight: bold; color: #1e293b; margin-bottom: 15px; }
                .password-label { font-size: 13px; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }
                .password-value { font-family: 'Courier New', Courier, monospace; font-size: 22px; font-weight: bold; color: #2563eb; background-color: #eff6ff; border: 1px dashed #bfdbfe; padding: 10px 15px; border-radius: 6px; display: inline-block; letter-spacing: 1px; }
                .btn-container { text-align: center; margin: 30px 0; }
                .btn { background-color: #0056b3; color: #ffffff !important; text-decoration: none; padding: 12px 28px; font-weight: bold; border-radius: 6px; display: inline-block; font-size: 15px; }
                .security-note { background-color: #fffbebf5; border-left: 4px solid #f59e0b; padding: 12px 15px; border-radius: 0 4px 4px 0; font-size: 13px; color: #78350f; margin: 20px 0; }
                .footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #eef2f5; font-size: 13px; color: #94a3b8; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2>Kedves %s!</h2>
                </div>
                
                <p>Kérésedre új jelszót generáltunk a fiókodhoz. Az új belépési adataidat alább találod:</p>
                
                <div class="password-card">
                    <div class="username-label">Felhasználónév</div>
                    <div class="username-value">%s</div>
                    
                    <div class="password-label">Új jelszó</div>
                    <div class="password-value">%s</div>
                </div>

                <div class="btn-container">
                    <a href="https://dukapp.bcc-ktk.org/login" class="btn" target="_blank">Bejelentkezés a felületre</a>
                </div>

                <div class="security-note">
                    <strong>Biztonsági figyelmeztetés:</strong> Belépés után javasoljuk, hogy az első adandó alkalommal változtasd meg a jelszavadat a profilbeállításoknál!
                </div>

                <div class="footer">
                    Üdvözlettel,<br>
                    <strong>MyShare csapat</strong>
                </div>
            </div>
        </body>
        </html>
        """,
                user.getFullName(),
                user.getUsername(),
                newPassword
        );
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


    private String createStatusMailBody(User user, Integer currentCredit, double currentStatus, Integer creditToBeOnTrack, Round round) {
        String endDate = round.getEndDateTime().toLocalDate().toString();
        String endTime = round.getEndDateTime().toLocalTime().toString();

        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333; }
                .container { max-width: 600px; background-color: #ffffff; border-radius: 8px; margin: 0 auto; padding: 30px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }
                .header { border-bottom: 2px solid #eef2f5; padding-bottom: 15px; margin-bottom: 20px; }
                .header h2 { color: #0056b3; margin: 0; font-size: 22px; }
                .card { background-color: #f8fafc; border-left: 4px solid #0056b3; border-radius: 4px; padding: 15px 20px; margin: 20px 0; }
                .stat-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px dashed #e2e8f0; }
                .stat-row:last-child { border-bottom: none; }
                .stat-label { color: #64748b; font-weight: 500; }
                .stat-value { font-weight: bold; color: #1e293b; }
                .highlight { color: #2563eb; font-size: 1.1em; }
                .deadline-box { background-color: #eff6ff; border: 1px solid #bfdbfe; border-radius: 6px; padding: 12px 15px; text-align: center; margin-top: 20px; color: #1e40af; font-weight: bold; }
                .footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #eef2f5; font-size: 13px; color: #94a3b8; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2>Kedves %s!</h2>
                </div>
                
                <p>Az alábbiakban találod a **MyShare** egyenleged és státuszod aktuális összefoglalóját:</p>
                
                <div class="card">
                    <table width="100%%" cellspacing="0" cellpadding="6">
                        <tr>
                            <td style="color: #64748b;">Jelenlegi státusz:</td>
                            <td align="right" style="font-weight: bold; color: #2563eb;">%s (%.2f%%)</td>
                        </tr>
                        <tr>
                            <td style="color: #64748b;">OnTrackhez szükséges összeg:</td>
                            <td align="right" style="font-weight: bold;">%s</td>
                        </tr>
                    </table>
                </div>

                <div class="deadline-box">
                    Befizetési határidő: %s, %s
                </div>

                <div class="footer">
                    Üdvözlettel,<br>
                    <strong>MyShare csapat</strong>
                </div>
            </div>
        </body>
        </html>
        """,
                user.getFullName(),
                currentCredit,
                currentStatus,
                creditToBeOnTrack,
                endDate,
                endTime
        );
    }

}
