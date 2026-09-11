# DevMetrics — Definição do MVP

## Problema

Desenvolvedores em início de carreira não têm visibilidade da própria evolução. O gráfico
de contribuições do GitHub mede apenas commits, o que incentiva ruído (commits vazios) e
ignora atividades que realmente representam crescimento: testes, documentação, bugs
corrigidos, estudo e tecnologias novas.

## Proposta

Plataforma que agrega atividades de desenvolvimento (manuais + importadas do GitHub),
converte em um **Dev Score** transparente e devolve ao usuário:

- um calendário de intensidade (estilo GitHub) baseado em atividade real;
- um score composto que premia consistência, variedade e aprendizado;
- um empurrão diário pequeno e concreto (anti-procrastinação);
- sugestões de próxima tecnologia a experimentar.

## Escopo do MVP (v1)

Dentro:

1. Cadastro e login próprios (e-mail + senha, JWT).
2. CRUD de projetos.
3. CRUD de atividades manuais (commit, bug, teste, feature, PR, issue, doc, estudo).
4. Catálogo de tecnologias + vínculo com projetos e atividades.
5. Motor de pontuação configurável com regras persistidas em banco.
6. Dev Score composto (volume, consistência, variedade, aprendizado, diversidade).
7. Dashboard: score, streak, totais, calendário anual, top tecnologias.
8. Conexão com GitHub (OAuth) e sincronização sob demanda.
9. Classificação automática das atividades vindas do GitHub (heurística, sem IA).
10. Conquistas (achievements) com regras determinísticas.
11. Desafio de Hoje (regras, sem IA).
12. Próximo Passo (regras de co-ocorrência de tecnologias).
13. Histórico mensal.
14. Swagger/OpenAPI, Docker Compose, testes.

Fora do MVP (backlog consciente):

- IA para gerar desafios e recomendações;
- webhooks do GitHub (sync é sob demanda + agendado);
- integração com GitLab/Bitbucket;
- times, ranking social, comparação entre usuários;
- app mobile;
- migração para React (a arquitetura já permite).

## Critérios de aceite do MVP

- Usuário cria conta, conecta GitHub, roda sync e vê o calendário preenchido.
- Dev Score é explicável: a API devolve cada componente e o peso usado.
- Nenhuma regra de pontuação está hardcoded no código de negócio.
- Cobertura de testes >= 80% no módulo de scoring.
- `docker compose up` sobe API + banco + frontend.

## Personas

- **Estudante (principal)**: quer provar evolução e manter constância.
- **Dev júnior**: quer identificar lacunas técnicas (nunca escreveu teste, nunca usou Docker).
