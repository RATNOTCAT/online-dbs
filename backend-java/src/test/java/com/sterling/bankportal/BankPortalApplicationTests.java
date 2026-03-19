package com.sterling.bankportal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.AccountRepository;
import com.sterling.bankportal.repo.UserRepository;
import com.sterling.bankportal.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BankPortalApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void contextLoads() {
    }

    @Test
    void adminSummaryUnderUserNamespaceAllowsAdminAccess() throws Exception {
        User user = userRepository.findByEmailIgnoreCase("admin@vibebank.com").orElseGet(() -> {
            User newUser = new User();
            newUser.setName("Vibe Admin");
            newUser.setUsername("vibeadmin");
            newUser.setEmail("admin@vibebank.com");
            newUser.setRole("admin");
            newUser.setPasswordHash(passwordEncoder.encode("Admin@123"));
            return userRepository.save(newUser);
        });
        user.setRole("admin");
        user.setPasswordHash(passwordEncoder.encode("Admin@123"));
        userRepository.save(user);

        if (accountRepository.findFirstByUserId(user.getId()).isEmpty()) {
            Account account = new Account();
            account.setUserId(user.getId());
            account.setAccountNumber("4082999911112222");
            account.setAccountType("Savings");
            account.setBalance(100000.0);
            accountRepository.save(account);
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        mockMvc.perform(get("/api/user/admin-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.summary.total_users").exists());
    }
}
