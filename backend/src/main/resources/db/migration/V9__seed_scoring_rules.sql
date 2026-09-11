-- Regras padrao de pontuacao (user_id NULL = global).
--
-- daily_cap  limita quantas atividades do tipo pontuam por dia.
-- diminishing aplica base/n na n-esima atividade do mesmo tipo no mesmo dia.
-- Juntos, sao o que impede que repetir a mesma acao seja o caminho facil para pontuar.

INSERT INTO scoring_rules (user_id, activity_type, base_points, daily_cap, diminishing, active, created_at) VALUES
    (NULL, 'COMMIT',        5.00, 10, TRUE,  TRUE, now()),
    (NULL, 'BUG_FIX',      10.00,  6, TRUE,  TRUE, now()),
    (NULL, 'TEST',          8.00,  8, TRUE,  TRUE, now()),
    (NULL, 'PULL_REQUEST', 10.00,  5, TRUE,  TRUE, now()),
    (NULL, 'ISSUE',         4.00,  5, TRUE,  TRUE, now()),
    (NULL, 'FEATURE',      15.00,  4, TRUE,  TRUE, now()),
    (NULL, 'REFACTOR',      7.00,  4, TRUE,  TRUE, now()),
    (NULL, 'CODE_REVIEW',   6.00,  5, TRUE,  TRUE, now()),
    (NULL, 'DOCUMENTATION', 6.00,  4, TRUE,  TRUE, now()),
    (NULL, 'STUDY',         5.00,  3, TRUE,  TRUE, now()),
    (NULL, 'DEPLOY',        8.00,  3, TRUE,  TRUE, now()),
    (NULL, 'NEW_PROJECT',  25.00,  2, FALSE, TRUE, now());
