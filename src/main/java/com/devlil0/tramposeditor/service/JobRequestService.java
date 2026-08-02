package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.JobInterestEntity;
import com.devlil0.tramposeditor.database.model.JobRequestEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IEditorProfileRepository;
import com.devlil0.tramposeditor.database.repository.IJobInterestRepository;
import com.devlil0.tramposeditor.database.repository.IJobRequestRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.*;
import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.JobInterestStatus;
import com.devlil0.tramposeditor.enums.JobRequestStatus;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.ForbiddenException;
import com.devlil0.tramposeditor.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * "Postar pedido rápido" (FUNDACAO §10.2) — a ponta da demanda.
 * <p>
 * Regras que o modelo assume: qualquer conta pode publicar (todo mundo é cliente);
 * só quem tem perfil de editor publicado pode demonstrar interesse; e o contato das
 * duas pontas só é revelado depois que o cliente aceita — enquanto o chat (Bloco 5)
 * não existe, é o aceite que faz esse papel.
 */
@Service
@RequiredArgsConstructor
public class JobRequestService {

    private final IJobRequestRepository jobRequestRepository;
    private final IJobInterestRepository jobInterestRepository;
    private final IEditorProfileRepository editorProfileRepository;
    private final IUserRepository userRepository;
    private final ConversationService conversationService;

    // ---------- Lado do cliente ----------

    @Transactional
    public JobRequestDTO create(UUID clientId, JobRequestCreateDTO dto){
        UserEntity client = userRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException(clientId));

        JobRequestEntity job = JobRequestEntity.builder()
                .client(client)
                .title(dto.title().trim())
                .description(dto.description().trim())
                .niche(dto.niche())
                .budgetCents(dto.budgetCents())
                .deadlineDays(dto.deadlineDays())
                .referenceUrl(trimToNull(dto.referenceUrl()))
                .status(JobRequestStatus.OPEN)
                .build();

        return toOwnerDTO(jobRequestRepository.save(job));
    }

    @Transactional(readOnly = true)
    public List<JobRequestDTO> listMine(UUID clientId){
        return jobRequestRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(this::toOwnerDTO)
                .toList();
    }

    @Transactional
    public JobRequestDTO close(UUID clientId, UUID jobId){
        JobRequestEntity job = loadOwnJob(clientId, jobId);
        job.setStatus(JobRequestStatus.CLOSED);
        return toOwnerDTO(job);
    }

    /**
     * Aceitar abre a conversa do pedido — a segunda porta da caixa de entrada
     * (ESBOCO §7). É aqui que o Bloco 5 encosta no Bloco 7.
     * <p>
     * O e-mail das duas pontas continua sendo revelado por ora. A intenção registrada
     * é que a conversa <i>substitua</i> essa revelação (FUNDACAO §11.9), mas a troca é
     * feita depois que o chat estiver rodando na mão do usuário: tirar o e-mail antes
     * disso deixaria quem já usa o fluxo sem caminho nenhum.
     */
    @Transactional
    public JobRequestDTO acceptInterest(UUID clientId, UUID jobId, UUID interestId){
        JobRequestEntity job = loadOwnJob(clientId, jobId);

        JobInterestEntity interest = job.getInterests().stream()
                .filter(i -> i.getId().equals(interestId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Interesse não encontrado: " + interestId));

        interest.setStatus(JobInterestStatus.ACCEPTED);
        conversationService.openForJob(job, interest.getEditor());
        return toOwnerDTO(job);
    }

    // ---------- Lado do editor ----------

    @Transactional(readOnly = true)
    public List<JobRequestFeedDTO> feed(UUID editorUserId, EditorNiche niche){
        List<JobRequestEntity> jobs = niche == null
                ? jobRequestRepository.findByStatusOrderByCreatedAtDesc(JobRequestStatus.OPEN)
                : jobRequestRepository.findByStatusAndNicheOrderByCreatedAtDesc(JobRequestStatus.OPEN, niche);

        return jobs.stream()
                // O próprio pedido não aparece no feed de quem o publicou.
                .filter(job -> !job.getClient().getId().equals(editorUserId))
                .map(job -> toFeedDTO(job, editorUserId))
                .toList();
    }

    @Transactional
    public JobRequestFeedDTO expressInterest(UUID editorUserId, UUID jobId, JobInterestCreateDTO dto){
        JobRequestEntity job = jobRequestRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado: " + jobId));

        if (!job.isOpen()) {
            throw new BusinessException("Esse pedido já foi fechado pelo cliente");
        }
        if (job.getClient().getId().equals(editorUserId)) {
            throw new ForbiddenException("Você não pode se candidatar ao próprio pedido");
        }
        if (editorProfileRepository.findByUserId(editorUserId).isEmpty()) {
            throw new ForbiddenException("Publique seu perfil de editor antes de se candidatar");
        }
        if (jobInterestRepository.existsByJobRequestIdAndEditorId(jobId, editorUserId)) {
            throw new BusinessException("Você já demonstrou interesse nesse pedido");
        }

        UserEntity editor = userRepository.findById(editorUserId)
                .orElseThrow(() -> new NotFoundException(editorUserId));

        job.addInterest(JobInterestEntity.builder()
                .editor(editor)
                .message(dto.message().trim())
                .status(JobInterestStatus.PENDING)
                .build());

        jobRequestRepository.save(job);
        return toFeedDTO(job, editorUserId);
    }

    /** Pedidos em que este editor se candidatou — inclusive os já fechados. */
    @Transactional(readOnly = true)
    public List<JobRequestFeedDTO> myInterests(UUID editorUserId){
        return jobInterestRepository.findByEditorIdOrderByCreatedAtDesc(editorUserId).stream()
                .map(interest -> toFeedDTO(interest.getJobRequest(), editorUserId))
                .toList();
    }

    // ---------- Helpers ----------

    private JobRequestEntity loadOwnJob(UUID clientId, UUID jobId){
        JobRequestEntity job = jobRequestRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado: " + jobId));

        if (!job.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("Esse pedido não é seu");
        }
        return job;
    }

    private JobRequestDTO toOwnerDTO(JobRequestEntity job){
        List<JobInterestDTO> interests = job.getInterests().stream()
                .map(this::toInterestDTO)
                .toList();

        return new JobRequestDTO(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getNiche(),
                job.getBudgetCents(),
                job.getDeadlineDays(),
                job.getReferenceUrl(),
                job.getStatus(),
                interests,
                job.getCreatedAt()
        );
    }

    private JobInterestDTO toInterestDTO(JobInterestEntity interest){
        UserEntity editor = interest.getEditor();
        boolean accepted = interest.getStatus() == JobInterestStatus.ACCEPTED;

        String slug = editorProfileRepository.findByUserId(editor.getId())
                .map(profile -> profile.getSlug())
                .orElse(null);

        return new JobInterestDTO(
                interest.getId(),
                editor.getDisplayName(),
                slug,
                interest.getMessage(),
                interest.getStatus(),
                accepted ? editor.getEmail() : null,
                interest.getCreatedAt()
        );
    }

    private JobRequestFeedDTO toFeedDTO(JobRequestEntity job, UUID editorUserId){
        JobInterestStatus myStatus = job.getInterests().stream()
                .filter(i -> i.getEditor().getId().equals(editorUserId))
                .map(JobInterestEntity::getStatus)
                .findFirst()
                .orElse(null);

        boolean accepted = myStatus == JobInterestStatus.ACCEPTED;

        return new JobRequestFeedDTO(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getNiche(),
                job.getBudgetCents(),
                job.getDeadlineDays(),
                job.getReferenceUrl(),
                job.getClient().getDisplayName(),
                job.getInterests().size(),
                myStatus,
                accepted ? job.getClient().getEmail() : null,
                job.getCreatedAt()
        );
    }

    private String trimToNull(String value){
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
