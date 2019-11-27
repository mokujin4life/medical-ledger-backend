package com.mokujin.user.service.impl;

import com.mokujin.user.model.Contact;
import com.mokujin.user.model.ProcessedUserCredentials;
import com.mokujin.user.model.User;
import com.mokujin.user.model.chat.Message;
import com.mokujin.user.model.document.Document;
import com.mokujin.user.model.notification.Notification;
import com.mokujin.user.model.notification.NotificationCollector;
import com.mokujin.user.model.notification.SystemNotification;
import com.mokujin.user.model.notification.extention.*;
import com.mokujin.user.model.presentation.Proof;
import com.mokujin.user.model.record.HealthRecord;
import com.mokujin.user.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.mokujin.user.model.notification.Notification.Type.CONNECTION;
import static com.mokujin.user.model.notification.Notification.Type.INVITATION;
import static com.mokujin.user.model.notification.NotificationConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final RedissonClient redissonClient;

    @Override
    public NotificationCollector getNotifications(String nationalNumber) {

        RList<Message> messages = redissonClient.getList("messages_" + nationalNumber);
        List<ChatNotification> messageNotifications = messages.stream()
                .map(ChatNotification::new)
                .collect(Collectors.toList());

        List<SystemNotification> connectionNotifications = redissonClient.getMap("connections_" + nationalNumber)
                .values()
                .stream()
                .map(n -> (SystemNotification) n)
                .collect(Collectors.toList());

        List<SystemNotification> invitationNotifications = redissonClient.getMap("invitations_" + nationalNumber)
                .values()
                .stream()
                .map(n -> (SystemNotification) n)
                .collect(Collectors.toList());

        List<PresentationNotification> presentationNotifications = redissonClient.getMap("presentations_" + nationalNumber)
                .values()
                .stream()
                .map(n -> (PresentationNotification) n)
                .collect(Collectors.toList());

        List<ProofNotification> proofNotifications = redissonClient.getMap("proofs_" + nationalNumber)
                .values()
                .stream()
                .map(n -> (ProofNotification) n)
                .collect(Collectors.toList());

        List<HealthNotification> healthNotifications = redissonClient.getMap("health_" + nationalNumber)
                .values()
                .stream()
                .map(n -> (HealthNotification) n)
                .collect(Collectors.toList());

        List<OfferNotification> offerNotifications = redissonClient.getMap("offers_" + nationalNumber)
                .values()
                .stream()
                .map(n -> (OfferNotification) n)
                .collect(Collectors.toList());

        List<AskNotification> askNotifications = redissonClient.getMap("asks_" + nationalNumber)
                .values()
                .stream()
                .map(n -> (AskNotification) n)
                .collect(Collectors.toList());

        List<DocumentNotification> documentNotifications = redissonClient.getMap("documents_" + nationalNumber)
                .values()
                .stream()
                .map(n -> (DocumentNotification) n)
                .collect(Collectors.toList());

        return NotificationCollector.builder()
                .messages(messageNotifications)
                .connections(connectionNotifications)
                .invitations(invitationNotifications)
                .presentations(presentationNotifications)
                .proofs(proofNotifications)
                .health(healthNotifications)
                .offers(offerNotifications)
                .asks(askNotifications)
                .documents(documentNotifications)
                .build();
    }

    @Override
    public Notification addInviteNotification(String publicKey, String privateKey, Contact doctor, User patient) {
        String patientNumber = patient.getNationalNumber();
        String doctorNumber = doctor.getNationalNumber();

        ProcessedUserCredentials patientCredentials = ProcessedUserCredentials.builder()
                .publicKey(publicKey)
                .privateKey(privateKey)
                .build();
        RMap<String, ProcessedUserCredentials> credentials = redissonClient.getMap("credentials");
        credentials.put(doctorNumber + patientNumber, patientCredentials);

        RMap<String, SystemNotification> doctorNotifications = redissonClient.getMap("connections_" + doctorNumber);
        SystemNotification connectionNotification = new SystemNotification(new Date().getTime(), CONNECTION,
                Contact.builder()
                        .contactName(patient.getLastName() + " " + patient.getFirstName() + " " + patient.getFatherName())
                        .photo(patient.getPhoto())
                        .nationalNumber(patientNumber)
                        .isVisible(true)
                        .build(), "", "", CONNECTION_CONTENT_EN, CONNECTION_CONTENT_UKR);
        doctorNotifications.put(publicKey, connectionNotification);

        RMap<String, SystemNotification> patientNotifications = redissonClient.getMap("invitations_" + patientNumber);
        SystemNotification invitationNotification = new SystemNotification(new Date().getTime(), INVITATION,
                doctor, "", "", INVITATION_CONTENT_EN, INVITATION_CONTENT_UKR);
        patientNotifications.put(doctorNumber, invitationNotification);

        return invitationNotification;
    }

    @Override
    public ProcessedUserCredentials removeInviteNotification(String doctorNumber, String patientNumber) {
        RMap<String, ProcessedUserCredentials> credentials = redissonClient.getMap("credentials");
        ProcessedUserCredentials patientCredentials = credentials.get(doctorNumber + patientNumber);
        credentials.remove(doctorNumber + patientNumber);

        RMap<String, SystemNotification> doctorNotifications = redissonClient.getMap("connections_" + doctorNumber);
        doctorNotifications.remove(patientCredentials.getPublicKey());

        RMap<String, SystemNotification> patientNotifications = redissonClient.getMap("invitations_" + patientNumber);
        patientNotifications.remove(doctorNumber);

        return patientCredentials;
    }

    @Override
    public Notification addMessage(String connectionNumber, Message message) {
        RList<Message> messages = redissonClient.getList("messages_" + connectionNumber);
        messages.add(message);

        return new ChatNotification(message);
    }

    @Override
    public void removeMessage(String nationalNumber, Message message) {
        RList<Message> messages = redissonClient.getList("messages_" + nationalNumber);
        messages.remove(message);
    }

    @Override
    public Notification addPresentationNotification(User user, List<String> presentationAttributes,
                                                    String documentType, String connectionNumber) {

        RMap<String, PresentationNotification> presentationNotifications = redissonClient.getMap("presentations_" + connectionNumber);
        String nationalNumber = user.getNationalNumber();
        PresentationNotification presentationNotification = new PresentationNotification(new Date().getTime(),
                Contact.builder()
                        .contactName(user.getLastName() + " " + user.getFirstName() + " " + user.getFatherName())
                        .photo(user.getPhoto())
                        .nationalNumber(nationalNumber)
                        .isVisible(true)
                        .build(), PRESENTATION_TITLE_EN, PRESENTATION_TITLE_UKR, PRESENTATION_CONTENT_EN,
                PRESENTATION_CONTENT_UKR, documentType, presentationAttributes);
        presentationNotifications.put(nationalNumber, presentationNotification);

        return presentationNotification;
    }

    @Override
    public void removePresentationNotification(User user, String connectionNumber) {
        String nationalNumber = user.getNationalNumber();
        RMap<String, PresentationNotification> presentationNotifications = redissonClient.getMap("presentations_" + nationalNumber);
        presentationNotifications.remove(connectionNumber);
    }

    @Override
    public Notification addProofNotification(User user, Proof proof, String connectionNumber) {
        RMap<String, ProofNotification> proofNotifications = redissonClient.getMap("proofs_" + connectionNumber);
        String nationalNumber = user.getNationalNumber();
        ProofNotification proofNotification = new ProofNotification(new Date().getTime(),
                Contact.builder()
                        .contactName(user.getLastName() + " " + user.getFirstName() + " " + user.getFatherName())
                        .photo(user.getPhoto())
                        .nationalNumber(nationalNumber)
                        .isVisible(true)
                        .build(), PROOF_TITLE_EN, PROOF_TITLE_UKR, PROOF_CONTENT_EN, PROOF_CONTENT_UKR, proof);
        proofNotifications.put(nationalNumber, proofNotification);

        return proofNotification;
    }

    @Override
    public void removeProofNotification(String nationalNumber, String connectionNumber) {
        RMap<String, ProofNotification> proofNotifications = redissonClient.getMap("proofs_" + nationalNumber);
        proofNotifications.remove(connectionNumber);
    }

    @Override
    public Notification addHealthNotification(User user, HealthRecord record, String connectionNumber) {
        RMap<String, HealthNotification> healthNotifications = redissonClient.getMap("health_" + connectionNumber);
        String nationalNumber = user.getNationalNumber();
        HealthNotification healthNotification = new HealthNotification(new Date().getTime(),
                Contact.builder()
                        .contactName(user.getLastName() + " " + user.getFirstName() + " " + user.getFatherName())
                        .photo(user.getPhoto())
                        .nationalNumber(nationalNumber)
                        .isVisible(true)
                        .build(), HEALTH_TITLE_EN, HEALTH_TITLE_UKR, HEALTH_CONTENT_EN, HEALTH_CONTENT_UKR, record);
        healthNotifications.put(nationalNumber, healthNotification);

        return healthNotification;
    }

    @Override
    public void removeHealthNotification(String nationalNumber, String connectionNumber) {
        RMap<String, HealthNotification> healthNotifications = redissonClient.getMap("health_" + nationalNumber);
        healthNotifications.remove(connectionNumber);
    }

    @Override
    public Notification addOfferNotification(String publicKey, String privateKey, User doctor,
                                             Document document, String patientNumber) {

        String doctorNumber = doctor.getNationalNumber();

        ProcessedUserCredentials patientCredentials = ProcessedUserCredentials.builder()
                .publicKey(publicKey)
                .privateKey(privateKey)
                .build();
        RMap<String, ProcessedUserCredentials> credentials = redissonClient.getMap("credentials");
        credentials.put(patientNumber + doctorNumber, patientCredentials);

        RMap<String, OfferNotification> offerNotifications = redissonClient.getMap("offers_" + patientNumber);
        String nationalNumber = doctor.getNationalNumber();
        OfferNotification offerNotification = new OfferNotification(new Date().getTime(),
                Contact.builder()
                        .contactName(doctor.getLastName() + " " + doctor.getFirstName() + " " + doctor.getFatherName())
                        .photo(doctor.getPhoto())
                        .nationalNumber(nationalNumber)
                        .isVisible(true)
                        .build(), OFFER_TITLE_EN, OFFER_TITLE_UKR, OFFER_CONTENT_EN, OFFER_CONTENT_UKR, document);
        offerNotifications.put(publicKey, offerNotification);

        return offerNotification;
    }

    @Override
    public ProcessedUserCredentials removeOfferNotification(String patientNumber, String doctorNumber) {

        RMap<String, ProcessedUserCredentials> credentials = redissonClient.getMap("credentials");
        ProcessedUserCredentials doctorCredentials = credentials.get(patientNumber + doctorNumber);
        credentials.remove(patientNumber + doctorNumber);

        RMap<String, OfferNotification> offerNotifications = redissonClient.getMap("offers_" + patientNumber);
        offerNotifications.remove(doctorCredentials.getPublicKey());

        return doctorCredentials;
    }

    @Override
    public Notification addAskNotification(User user, List<String> keywords, String connectionNumber) {
        RMap<String, AskNotification> askNotifications = redissonClient.getMap("asks_" + connectionNumber);
        String nationalNumber = user.getNationalNumber();
        AskNotification askNotification = new AskNotification(new Date().getTime(),
                Contact.builder()
                        .contactName(user.getLastName() + " " + user.getFirstName() + " " + user.getFatherName())
                        .photo(user.getPhoto())
                        .nationalNumber(nationalNumber)
                        .isVisible(true)
                        .build(), ASK_TITLE_EN, ASK_TITLE_UKR, ASK_CONTENT_EN, ASK_CONTENT_UKR, keywords);
        askNotifications.put(nationalNumber, askNotification);

        return askNotification;
    }

    @Override
    public void removeAskNotification(String nationalNumber, String connectionNumber) {
        RMap<String, AskNotification> askNotifications = redissonClient.getMap("asks_" + nationalNumber);
        askNotifications.remove(connectionNumber);
    }

    @Override
    public Notification addDocumentNotification(User user, Document document, String connectionNumber) {
        RMap<String, DocumentNotification> documentNotifications = redissonClient.getMap("documents_" + connectionNumber);
        String nationalNumber = user.getNationalNumber();
        DocumentNotification documentNotification = new DocumentNotification(new Date().getTime(),
                Contact.builder()
                        .contactName(user.getLastName() + " " + user.getFirstName() + " " + user.getFatherName())
                        .photo(user.getPhoto())
                        .nationalNumber(nationalNumber)
                        .isVisible(true)
                        .build(), DOCUMENT_TITLE_EN, DOCUMENT_TITLE_UKR, DOCUMENT_CONTENT_EN, DOCUMENT_CONTENT_UKR, document);
        documentNotifications.put(nationalNumber, documentNotification);

        return documentNotification;
    }

    @Override
    public void removeDocumentNotification(String nationalNumber, String connectionNumber) {
        RMap<String, DocumentNotification> documentNotifications = redissonClient.getMap("documents_" + nationalNumber);
        documentNotifications.remove(connectionNumber);
    }


}
