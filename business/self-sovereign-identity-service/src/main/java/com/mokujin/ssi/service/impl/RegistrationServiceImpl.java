package com.mokujin.ssi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mokujin.ssi.model.exception.BusinessException;
import com.mokujin.ssi.model.exception.extention.LedgerException;
import com.mokujin.ssi.model.government.KnownIdentity;
import com.mokujin.ssi.model.government.document.Certificate;
import com.mokujin.ssi.model.government.document.Diploma;
import com.mokujin.ssi.model.government.document.NationalNumber;
import com.mokujin.ssi.model.government.document.NationalPassport;
import com.mokujin.ssi.model.internal.Contact;
import com.mokujin.ssi.model.internal.Identity;
import com.mokujin.ssi.model.internal.Pseudonym;
import com.mokujin.ssi.model.internal.Schema;
import com.mokujin.ssi.model.user.request.UserRegistrationDetails;
import com.mokujin.ssi.model.user.response.User;
import com.mokujin.ssi.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mokujin.ssi.model.internal.Role.DOCTOR;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.proverCreateMasterSecret;
import static org.hyperledger.indy.sdk.did.Did.createAndStoreMyDid;
import static org.hyperledger.indy.sdk.did.DidResults.CreateAndStoreMyDidResult;
import static org.hyperledger.indy.sdk.ledger.Ledger.buildNymRequest;
import static org.hyperledger.indy.sdk.ledger.Ledger.signAndSubmitRequest;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final ObjectMapper objectMapper;
    private final VerificationService verificationService;
    private final WalletService walletService;
    private final IdentityService identityService;
    private final UserService userService;
    private final CredentialService credentialService;

    @Qualifier("government")
    private final Identity government;
    @Qualifier("steward")
    private final Identity steward;

    private final Pool pool;

    @Qualifier("passportSchema")
    private final Schema passportSchema;
    @Qualifier("nationalNumberSchema")
    private final Schema nationalNumberSchema;
    @Qualifier("certificateSchema")
    private final Schema certificateSchema;
    @Qualifier("diplomaSchema")
    private final Schema diplomaSchema;

    @Override
    public User register(UserRegistrationDetails details, String publicKey, String privateKey) {

        Wallet governmentWallet = government.getWallet();
        try (Wallet userWallet = walletService.getOrCreateWallet(publicKey, privateKey)) {

            Identity userIdentity = identityService.findByWallet(userWallet);

            if (userIdentity.getCredentials().isEmpty()) {

                KnownIdentity knownIdentity = verificationService.verifyNewbie(details);

                CreateAndStoreMyDidResult governmentPseudonym = createAndStoreMyDid(
                        governmentWallet,
                        "{}")
                        .get();
                CreateAndStoreMyDidResult userPseudonym = createAndStoreMyDid(
                        userIdentity.getWallet(),
                        "{}")
                        .get();
                identityService.establishUserConnection(pool, government, governmentPseudonym, userPseudonym);

                if (knownIdentity.getRole().equals(DOCTOR)) {
                    this.grandVerinym(userIdentity, knownIdentity);
                }

                this.exchangeContacts(userIdentity, knownIdentity, governmentPseudonym, userPseudonym);

                this.issueCredentials(publicKey, userWallet, governmentPseudonym, knownIdentity);

                userIdentity = identityService.findByWallet(userWallet);
            }

            User user = userService.convert(userIdentity);
            log.info("'user={}'", user);
            return user;

        } catch (BusinessException e) {
            log.error("Exception was thrown: " + e);
            throw new LedgerException(e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Exception was thrown: " + e);
            throw new LedgerException(INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void grandVerinym(Identity userIdentity, KnownIdentity knownIdentity) throws Exception {
        CreateAndStoreMyDidResult verinym = createAndStoreMyDid(userIdentity.getWallet(), "{}").get();
        log.info("'verinym={}'", verinym);

        String firstName = knownIdentity.getNationalPassport().getFirstName();
        String lastName = knownIdentity.getNationalPassport().getLastName();
        String fatherName = knownIdentity.getNationalPassport().getFatherName();
        Contact selfContact = Contact.builder()
                .contactName(lastName + " " + firstName + " " + fatherName)
                .photo(knownIdentity.getNationalPassport().getImage())
                .nationalNumber(knownIdentity.getNationalNumber().getNumber())
                .isVisible(false)
                .isVerinym(true)
                .build();
        String selfContactJson = objectMapper.writeValueAsString(selfContact);
        Did.setDidMetadata(userIdentity.getWallet(), verinym.getDid(), selfContactJson).get();

        userIdentity.setVerinymDid(verinym.getDid());

        String nymRegisterTrustAnchorVerinym = buildNymRequest(
                steward.getVerinymDid(),
                verinym.getDid(),
                verinym.getVerkey(),
                null,
                "ENDORSER").get();
        log.info("'nymRegisterDoctorVerinym={}'", nymRegisterTrustAnchorVerinym);

        String nymRegisterTrustAnchorVerinymResponse = signAndSubmitRequest(
                pool,
                steward.getWallet(),
                steward.getVerinymDid(),
                nymRegisterTrustAnchorVerinym).get();
        log.info("'nymRegisterDoctorVerinymResponse={}'", nymRegisterTrustAnchorVerinymResponse);
    }

    void exchangeContacts(Identity userIdentity, KnownIdentity knownIdentity,
                          CreateAndStoreMyDidResult governmentPseudonym,
                          CreateAndStoreMyDidResult userForGovernmentPseudonym) throws Exception {
        Contact trustAnchorContactForUser = Contact.builder()
                .contactName("Government")
                .photo(government.getImage())
                .isVisible(false)
                .build();
        String trustAnchorContactForUserJson = objectMapper.writeValueAsString(trustAnchorContactForUser);
        System.out.println("trustAnchorContactForUserJson = " + trustAnchorContactForUserJson);
        Did.setDidMetadata(userIdentity.getWallet(), userForGovernmentPseudonym.getDid(), trustAnchorContactForUserJson).get();

        String firstName = knownIdentity.getNationalPassport().getFirstName();
        String lastName = knownIdentity.getNationalPassport().getLastName();
        String fatherName = knownIdentity.getNationalPassport().getFatherName();
        Contact userContactForTrustAnchor = Contact.builder()
                .contactName(lastName + " " + firstName + " " + fatherName)
                .photo(knownIdentity.getNationalPassport().getImage())
                .nationalNumber(knownIdentity.getNationalNumber().getNumber())
                .isVisible(false)
                .build();
        String userContactForTrustAnchorJson = objectMapper.writeValueAsString(userContactForTrustAnchor);
        System.out.println("userContactForTrustAnchorJson = " + userContactForTrustAnchorJson);
        Did.setDidMetadata(government.getWallet(), governmentPseudonym.getDid(), userContactForTrustAnchorJson).get();

        userIdentity.addPseudonym(Pseudonym.builder()
                .pseudonymDid(userForGovernmentPseudonym.getDid())
                .contact(trustAnchorContactForUser)
                .build());

        government.addPseudonym(Pseudonym.builder()
                .pseudonymDid(governmentPseudonym.getDid())
                .contact(userContactForTrustAnchor)
                .build());
    }

    void issueCredentials(String publicKey, Wallet userWallet,
                          CreateAndStoreMyDidResult governmentPseudonym,
                          KnownIdentity knownIdentity) throws Exception {

        String masterSecretId = proverCreateMasterSecret(userWallet, publicKey).get();

        String nationalNumberSchemaDefinitionId = nationalNumberSchema.getSchemaDefinitionId();
        String nationalNumberSchemaDefinition = nationalNumberSchema.getSchemaDefinition();
        NationalNumber nationalNumber = knownIdentity.getNationalNumber();

        credentialService.issueCredential(userWallet, government.getWallet(), governmentPseudonym.getDid(),
                nationalNumberSchemaDefinitionId, nationalNumberSchemaDefinition, nationalNumber, masterSecretId);

        String passportSchemaDefinitionId = passportSchema.getSchemaDefinitionId();
        String passportSchemaDefinition = passportSchema.getSchemaDefinition();
        NationalPassport nationalPassport = knownIdentity.getNationalPassport();

        credentialService.issueCredential(userWallet, government.getWallet(), governmentPseudonym.getDid(),
                passportSchemaDefinitionId, passportSchemaDefinition, nationalPassport, masterSecretId);

        if (knownIdentity.getRole().equals(DOCTOR)) {
            String diplomaSchemaDefinitionId = diplomaSchema.getSchemaDefinitionId();
            String diplomaSchemaDefinition = diplomaSchema.getSchemaDefinition();
            Diploma diploma = knownIdentity.getDiploma();

            credentialService.issueCredential(userWallet, government.getWallet(), governmentPseudonym.getDid(),
                    diplomaSchemaDefinitionId, diplomaSchemaDefinition, diploma, masterSecretId);

            String certificateSchemaDefinitionId = certificateSchema.getSchemaDefinitionId();
            String certificationSchemaDefinition = certificateSchema.getSchemaDefinition();
            List<Certificate> certificates = knownIdentity.getCertificates();

            for (Certificate certificate : certificates) {
                credentialService.issueCredential(userWallet, government.getWallet(), governmentPseudonym.getDid(),
                        certificateSchemaDefinitionId, certificationSchemaDefinition, certificate, masterSecretId);
            }
        }

    }
}
