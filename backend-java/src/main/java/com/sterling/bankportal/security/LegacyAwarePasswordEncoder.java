package com.sterling.bankportal.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class LegacyAwarePasswordEncoder implements PasswordEncoder {

    private static final Logger log = LoggerFactory.getLogger(LegacyAwarePasswordEncoder.class);
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(10);

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        if (isBcrypt(encodedPassword)) {
            return delegate.matches(rawPassword, encodedPassword);
        }
        if (isWerkzeugHash(encodedPassword)) {
            return verifyWithLegacyPython(rawPassword, encodedPassword);
        }
        try {
            return delegate.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException exception) {
            log.debug("Unsupported password hash format encountered");
            return false;
        }
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return encodedPassword != null && !isBcrypt(encodedPassword);
    }

    private boolean isBcrypt(String encodedPassword) {
        return encodedPassword.startsWith("$2a$")
                || encodedPassword.startsWith("$2b$")
                || encodedPassword.startsWith("$2y$");
    }

    private boolean isWerkzeugHash(String encodedPassword) {
        return encodedPassword.startsWith("scrypt:") || encodedPassword.startsWith("pbkdf2:");
    }

    private boolean verifyWithLegacyPython(CharSequence rawPassword, String encodedPassword) {
        for (String pythonCommand : candidatePythonCommands()) {
            Boolean verified = runWerkzeugCheck(pythonCommand, rawPassword, encodedPassword);
            if (verified != null) {
                return verified;
            }
        }
        log.warn("Legacy password hash detected but no compatible Python/Werkzeug runtime was available");
        return false;
    }

    private List<String> candidatePythonCommands() {
        List<String> commands = new ArrayList<>();
        commands.add("python");
        commands.add("py");
        return commands;
    }

    private Boolean runWerkzeugCheck(String pythonCommand, CharSequence rawPassword, String encodedPassword) {
        Process process = null;
        try {
            List<String> command = new ArrayList<>();
            command.add(pythonCommand);
            if ("py".equals(pythonCommand)) {
                command.add("-3");
            }
            command.add(legacyVerifierScript().toString());
            command.add(encodedPassword);
            command.add(rawPassword.toString());
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(PROCESS_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                if (process.exitValue() == 0 && output != null) {
                    return "true".equalsIgnoreCase(output.trim());
                }
                if (output != null) {
                    log.debug("Legacy password helper returned unexpected output: {}", output);
                }
            }
        } catch (IOException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return null;
    }

    private Path legacyVerifierScript() {
        Path script = Path.of(System.getProperty("user.dir"))
                .resolve("scripts/verify_legacy_password.py")
                .normalize()
                .toAbsolutePath();
        if (!Files.exists(script)) {
            throw new IllegalStateException("Legacy password verifier script is missing: " + script);
        }
        return script;
    }
}
