package com.ktk.dukappservice.service;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class UserFamilyImportService {
    private static final String USER_DATA_URL = "C:\\workspace\\private\\dukapp-service\\src\\main\\resources\\imports\\docs\\family_ids.csv";

    private UserService userService;

    public UserFamilyImportService(UserService userService) {
        this.userService = userService;
    }

    public boolean importUserFamilyIds() {
        try (FileInputStream fis = new FileInputStream(USER_DATA_URL);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.ISO_8859_1);
             BufferedReader br = new BufferedReader(isr)) {
            String line = br.readLine();

            while ((line = br.readLine()) != null) {
                String[] attr = line.split(";");
                int myShareID = (Integer.parseInt(attr[0]));

                int familyMyShareID = (Integer.parseInt(attr[1]));
                Optional<User> user = userService.findByMyShareId((long) myShareID);

                Optional<User> family = userService.findByMyShareId((long) familyMyShareID);
                if (user.isPresent() && family.isPresent()) {
                    user.get().setFamilyId(family.get().getId());
                    if (attr.length > 5) {
                        int spouseMyShareID = (Integer.parseInt(attr[5]));
                        Optional<User> spouse = userService.findByMyShareId((long) spouseMyShareID);
                        if (spouse.isPresent()) {
                            user.get().setSpouseId(spouse.get().getId());
                            spouse.get().setFamilyId(user.get().getId());
                            spouse.get().setSpouseId(user.get().getId());

                            userService.save(spouse.get());
                        }
                    }
                    userService.save(user.get());

                }
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
