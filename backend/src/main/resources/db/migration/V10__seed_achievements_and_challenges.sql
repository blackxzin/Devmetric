-- Conquistas. O code precisa bater com AchievementEvaluator.progressByCode().

INSERT INTO achievements (code, name, description, icon, category, threshold, created_at) VALUES
    ('FIRST_PROJECT',      'Primeiro projeto',      'Voce registrou seu primeiro projeto.',                        '🚀', 'INICIO',       1,   now()),
    ('FIRST_PR',           'Primeiro Pull Request', 'Voce abriu seu primeiro Pull Request.',                       '🔀', 'INICIO',       1,   now()),
    ('FIRST_ISSUE',        'Primeira issue',        'Voce abriu sua primeira issue.',                              '🗒️', 'INICIO',       1,   now()),
    ('FIRST_TEST',         'Primeiro teste',        'Voce escreveu seu primeiro teste automatizado.',              '🧪', 'QUALIDADE',    1,   now()),
    ('FIRST_DOC',          'Primeira documentacao', 'Voce documentou o proprio codigo pela primeira vez.',         '📘', 'QUALIDADE',    1,   now()),
    ('FIRST_BUG_FIX',      'Primeiro bug corrigido','Voce corrigiu seu primeiro bug.',                             '🐛', 'QUALIDADE',    1,   now()),
    ('FIRST_STUDY',        'Primeiro estudo',       'Voce registrou sua primeira sessao de estudo.',               '📚', 'APRENDIZADO',  1,   now()),
    ('FIRST_NEW_TECH',     'Primeira tecnologia',   'Voce registrou o uso da sua primeira tecnologia.',            '✨', 'APRENDIZADO',  1,   now()),
    ('FIRST_DOCKER',       'Primeiro projeto com Docker', 'Voce usou Docker pela primeira vez.',                   '🐳', 'APRENDIZADO',  1,   now()),
    ('STREAK_7',           '7 dias consecutivos',   'Voce manteve 7 dias seguidos de atividade.',                  '🔥', 'CONSISTENCIA', 7,   now()),
    ('STREAK_30',          '30 dias de atividade',  'Voce manteve 30 dias seguidos de atividade.',                 '🏆', 'CONSISTENCIA', 30,  now()),
    ('FULL_WEEK',          'Semana completa',       'Voce fez 7 tipos diferentes de atividade em 7 dias.',         '🌈', 'CONSISTENCIA', 7,   now()),
    ('POLYGLOT',           'Poliglota',             'Voce ja usou 5 linguagens de programacao diferentes.',        '🗺️', 'APRENDIZADO',  5,   now()),
    ('TOOLBELT',           'Caixa de ferramentas',  'Voce ja usou 5 ferramentas diferentes.',                      '🧰', 'APRENDIZADO',  5,   now()),
    ('TEST_MASTER',        'Cultura de teste',      'Voce registrou 25 testes.',                                   '🛡️', 'QUALIDADE',    25,  now()),
    ('TEN_PROJECTS',       '10 projetos',           'Voce chegou a 10 projetos registrados.',                      '📦', 'VOLUME',       10,  now()),
    ('HUNDRED_ACTIVITIES', '100 atividades',        'Voce registrou 100 atividades no DevMetrics.',                '💯', 'VOLUME',       100, now());

-- Templates do Desafio de Hoje.
-- Marcadores disponiveis: {projeto} e {dias}.
-- O tom e sempre de facilitar o comeco: tarefa pequena, concreta e com tempo estimado.

INSERT INTO challenge_templates (code, title, description_template, activity_type, estimated_minutes, trigger_type, difficulty, active, created_at) VALUES
    ('INACTIVE_SMALL_COMMIT', 'Voltar com um passo pequeno',
     'Voce esta ha {dias} dias sem atividade. Desafio de hoje: abra {projeto} e faca uma unica melhoria pequena, como renomear uma variavel confusa ou extrair um metodo.',
     'COMMIT', 15, 'INACTIVE_DAYS', 'FACIL', TRUE, now()),
    ('INACTIVE_README', 'Retomar pelo README',
     'Voce esta ha {dias} dias sem atividade. Desafio de hoje: escreva tres linhas no README de {projeto} explicando o que o projeto faz.',
     'DOCUMENTATION', 15, 'INACTIVE_DAYS', 'FACIL', TRUE, now()),
    ('INACTIVE_STUDY', 'Vinte minutos de estudo',
     'Voce esta ha {dias} dias sem atividade. Desafio de hoje: leia a documentacao de algo que voce ja usa em {projeto} e registre o que aprendeu.',
     'STUDY', 20, 'INACTIVE_DAYS', 'FACIL', TRUE, now()),

    ('STREAK_KEEP_COMMIT', 'Manter a sequencia',
     'Sua sequencia esta viva e hoje ainda nao tem atividade. Desafio de hoje: um commit pequeno em {projeto} ja garante o dia.',
     'COMMIT', 15, 'STREAK_KEEPER', 'FACIL', TRUE, now()),
    ('STREAK_KEEP_TEST', 'Fechar o dia com um teste',
     'Sua sequencia esta viva e hoje ainda nao tem atividade. Desafio de hoje: escreva um teste para a funcao mais recente de {projeto}.',
     'TEST', 20, 'STREAK_KEEPER', 'FACIL', TRUE, now()),

    ('NO_TESTS_ENDPOINT', 'Primeiro teste do endpoint',
     'Voce nao registrou nenhum teste nas ultimas duas semanas. Desafio de hoje: escreva um teste para o endpoint de login de {projeto}, cobrindo o caso de senha errada.',
     'TEST', 20, 'NO_TESTS', 'FACIL', TRUE, now()),
    ('NO_TESTS_UNIT', 'Um teste unitario',
     'Voce nao registrou nenhum teste nas ultimas duas semanas. Desafio de hoje: escolha a funcao mais importante de {projeto} e escreva um teste para o caminho feliz.',
     'TEST', 20, 'NO_TESTS', 'FACIL', TRUE, now()),
    ('NO_TESTS_EDGE', 'Teste do caso limite',
     'Voce nao registrou nenhum teste nas ultimas duas semanas. Desafio de hoje: escreva um teste para um caso de erro de {projeto} (entrada vazia, nula ou invalida).',
     'TEST', 25, 'NO_TESTS', 'MEDIO', TRUE, now()),

    ('NO_DOCS_README', 'README que explica',
     'Faz duas semanas que voce nao escreve documentacao. Desafio de hoje: adicione ao README de {projeto} a secao "Como rodar", com os comandos exatos.',
     'DOCUMENTATION', 20, 'NO_DOCS', 'FACIL', TRUE, now()),
    ('NO_DOCS_METHOD', 'Documentar a parte dificil',
     'Faz duas semanas que voce nao escreve documentacao. Desafio de hoje: escreva um comentario de tres linhas explicando o "porque" do trecho mais confuso de {projeto}.',
     'DOCUMENTATION', 15, 'NO_DOCS', 'FACIL', TRUE, now()),

    ('LOW_VARIETY_BUG', 'Sair do modo commit',
     'Quase toda a sua pontuacao vem de um tipo so de atividade. Desafio de hoje: encontre um bug pequeno em {projeto} e corrija.',
     'BUG_FIX', 30, 'LOW_VARIETY', 'MEDIO', TRUE, now()),
    ('LOW_VARIETY_REFACTOR', 'Uma refatoracao pequena',
     'Quase toda a sua pontuacao vem de um tipo so de atividade. Desafio de hoje: extraia uma funcao longa de {projeto} em duas menores.',
     'REFACTOR', 30, 'LOW_VARIETY', 'MEDIO', TRUE, now()),

    ('NO_NEW_TECH_DOCKER', 'Experimentar Docker',
     'Faz um mes que voce nao estreia nenhuma tecnologia. Desafio de hoje: crie um Dockerfile simples para {projeto} e rode a aplicacao com docker run.',
     'STUDY', 45, 'NO_NEW_TECH', 'MEDIO', TRUE, now()),
    ('NO_NEW_TECH_CI', 'Experimentar CI',
     'Faz um mes que voce nao estreia nenhuma tecnologia. Desafio de hoje: crie um workflow do GitHub Actions que roda os testes de {projeto} a cada push.',
     'STUDY', 40, 'NO_NEW_TECH', 'MEDIO', TRUE, now()),

    ('DEFAULT_SMALL_FEATURE', 'Uma melhoria concreta',
     'Voce esta em ritmo. Desafio de hoje: escolha a menor melhoria util de {projeto} e entregue ela inteira.',
     'FEATURE', 30, 'DEFAULT', 'MEDIO', TRUE, now()),
    ('DEFAULT_REVIEW', 'Reler o proprio codigo',
     'Voce esta em ritmo. Desafio de hoje: releia o codigo que voce escreveu ontem em {projeto} e melhore um nome mal escolhido.',
     'CODE_REVIEW', 20, 'DEFAULT', 'FACIL', TRUE, now()),
    ('DEFAULT_STUDY', 'Aprender algo pequeno',
     'Voce esta em ritmo. Desafio de hoje: leia sobre um conceito que voce usa em {projeto} sem entender direito, e registre o que aprendeu.',
     'STUDY', 25, 'DEFAULT', 'FACIL', TRUE, now());
