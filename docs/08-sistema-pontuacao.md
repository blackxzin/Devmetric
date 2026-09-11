# DevMetrics — Sistema de Pontuação

Dois níveis, propositalmente separados:

1. **Pontos da atividade** — quanto vale um evento isolado. Simples, configurável.
2. **Dev Score (0–1000)** — qualidade do comportamento ao longo do tempo. Composto.

Se existisse só o nível 1, 50 commits vazios dariam 250 pontos. É exatamente isso que o
nível 2 impede.

## Nível 1 — Pontos por atividade

### Regras padrão (tabela `scoring_rules`, `user_id = NULL`)

| ActivityType | base | dailyCap | diminishing |
|---|---|---|---|
| COMMIT | 5 | 10 | sim |
| BUG_FIX | 10 | 6 | sim |
| TEST | 8 | 8 | sim |
| PULL_REQUEST | 10 | 5 | sim |
| ISSUE | 4 | 5 | sim |
| FEATURE | 15 | 4 | sim |
| REFACTOR | 7 | 4 | sim |
| CODE_REVIEW | 6 | 5 | sim |
| DOCUMENTATION | 6 | 4 | sim |
| STUDY | 5 | 3 | sim |
| DEPLOY | 8 | 3 | sim |
| NEW_PROJECT | 25 | 2 | não |

Todos editáveis em `PUT /scoring/rules/{type}`.

### Fórmula

Para a **n-ésima** atividade do mesmo tipo, no mesmo dia, do mesmo usuário:

```
se n > dailyCap        →  pontos = 0
se diminishing = false →  pontos = base
senão                  →  pontos = base / n            (rendimento decrescente harmônico)

pontos_finais = round(pontos * (1 + bonusTecnologiaNova), 2)
bonusTecnologiaNova = 0.5  quando a technology_id da atividade é a primeira aparição
                          daquela tecnologia para o usuário (cria user_technologies)
```

Exemplo — 10 commits no mesmo dia:
```
5 + 2.50 + 1.67 + 1.25 + 1.00 + 0.83 + 0.71 + 0.63 + 0.56 + 0.50 = 14.65 pontos
```
Contra 50 pontos de uma soma linear. O 10º commit vale 10% do primeiro: continuar
commitando não é punido, mas deixa de ser o caminho fácil para pontuar.

Um dia com 1 commit + 1 teste + 1 doc = `5 + 8 + 6 = 19` pontos, mais que 10 commits.
**A variedade paga mais que a repetição.** Esse é o incentivo central do sistema.

### Onde os pontos são gravados

`activities.points` é congelado na criação. Mudar uma regra não reescreve o passado —
só vale para atividades novas, ou para o período explicitamente reprocessado por
`POST /scoring/recalculate`.

## Nível 2 — Dev Score (0–1000)

Janela: **últimos 90 dias**. Recalculado diariamente (`@Scheduled` 03:00) e após cada sync.

```
devScore = round( 400*Volume + 200*Consistência + 150*Variedade
                  + 150*Aprendizado + 100*Diversidade )
```
Cada componente é normalizado em `[0,1]`.

### Volume (peso 400)

```
metaTrimestral = weeklyGoalPoints * 13          // default 150 * 13 = 1950
Volume = min(1, pontosBrutos90d / metaTrimestral)
```
Satura na meta: acumular 10x a meta não dá 10x o score. Volume nunca é mais de 40% do total.

### Consistência (peso 200)

```
Consistência = 0.6 * (diasAtivos90 / 90) + 0.4 * min(1, streakAtual / 30)
```
Premia a distribuição, não a explosão. 30 dias ativos espalhados > 30 dias ativos seguidos
e depois 60 parados — porque o primeiro termo pesa mais que o segundo.

### Variedade (peso 150) — entropia de Shannon normalizada

```
p_i = pontos do tipo i / pontos totais (90d)
H   = -Σ p_i * log(p_i)
Variedade = H / log(numTiposDistintosUsados)      // 0 se usou 1 tipo só
```
Usar só COMMIT → Variedade ≈ 0 → perde 150 pontos. Distribuir entre commits, testes,
docs, PRs e bugs → Variedade ≈ 1. É a métrica anti-farm mais forte do modelo.

### Aprendizado (peso 150)

```
Aprendizado = 0.5 * min(1, techsNovas90d / 4)
            + 0.3 * min(1, estudosRegistrados90d / 12)
            + 0.2 * min(1, projetosNovos90d / 3)
```

### Diversidade tecnológica (peso 100)

```
Diversidade = 0.6 * min(1, techsDistintas90d / 8)
            + 0.4 * min(1, categoriasDistintas90d / 5)
```
Categorias: LANGUAGE, FRAMEWORK, DATABASE, TOOL, CLOUD, TESTING.
Oito frameworks JS da mesma categoria valem menos que Java + Spring + Postgres + Docker + JUnit.

### Faixas

| Score | Nível |
|---|---|
| 0–199 | Iniciando |
| 200–399 | Em construção |
| 400–599 | Ativo |
| 600–799 | Consistente |
| 800–1000 | Em evolução contínua |

### Transparência

`GET /score` devolve, para cada componente: valor bruto, normalizado, peso e pontos.
O frontend mostra isso como barras. O usuário sempre consegue responder
"por que meu score caiu?" sem ler o código.

## Nível de intensidade do calendário

Calculado por dia, em `daily_stats`, relativo ao **próprio usuário** (percentis dos últimos 90 dias):

```
level 0 : rawPoints == 0
level 1 : > 0   e <= p40
level 2 : > p40 e <= p70
level 3 : > p70 e <= p90
level 4 : > p90
```
Relativo, não absoluto: quem faz pouco ainda vê progresso; quem faz muito não fica verde-escuro
todo dia. Fallback nos primeiros 14 dias (sem amostra suficiente): cortes fixos em 5/15/30 pontos.

## Regras anti-gaming (resumo)

| Brecha | Defesa |
|---|---|
| Muitos commits pequenos | rendimento decrescente + dailyCap |
| Só um tipo de atividade | componente Variedade (entropia) |
| Rajada e sumiço | componente Consistência |
| Inflar regra própria pra 999 pontos | Volume satura na meta; peso máx. 40% |
| Reimportar o mesmo commit | índice único `(user_id, source, external_id)` |
| Atividade manual com data futura | validação: `occurredAt <= now` e `>= now - 1 ano` |
| Registrar "estudo" 50x por dia | dailyCap 3 + teto de 12 no componente Aprendizado |

## Testes (alvo: 90% neste módulo)

`PointsCalculatorTest`
- primeira atividade do dia vale `base`;
- quinta atividade com diminishing vale `base/5`;
- atividade além do `dailyCap` vale 0;
- bônus de tecnologia nova aplicado só na primeira vez.

`DevScoreCalculatorTest`
- usuário sem atividade → 0;
- usuário só com commits → Variedade = 0, score limitado;
- mesmo volume distribuído em 5 tipos → score maior que concentrado em 1 (teste central);
- 90 dias ativos + streak 30 → Consistência = 1.0;
- score nunca ultrapassa 1000 nem fica abaixo de 0 (property-based).
