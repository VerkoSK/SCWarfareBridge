# Essencium Investor Brief (Standalone)
Datum: 14. 03. 2026  
Verzia: Investor Information Pack v1

---

## 1) Executive Summary
Essencium ide z aktualneho weboveho riesenia na jednu modernu multiplatformu:
- Web (desktop + mobile web)
- iOS/Android aplikacia
- Jednotne API jadro, spolocna autentifikacia, spolocne data

Ciel projektu je odstranit technicke limity aktualneho stavu, zvysit bezpecnost, zvladnut vyssiu zataz a otvorit priestor pre rychly rast produktu.

---

## 2) Investicna teza v skratke
1. Jeden backend pre vsetky klienty znizuje prevadzkove aj vyvojove naklady.
2. Mobilna appka zvysuje retenciu a frekvenciu pouzivania.
3. API-first architektura skracuje cas uvedenia novych funkcii na trh.
4. Security-by-design znizuje riziko incidentov a reputacnych strat.

---

## 3) Co je dnes a co bude ciel
### Dnes
- Legacy PHP monolit, session-only auth, manualne release
- Vyssia zavislost medzi modulmi a obmedzena skala

### Cielovy stav
- TypeScript platforma (NestJS backend, Next.js web, React Native mobile)
- OAuth2/OIDC + JWT, API gateway, central observability
- Strangler migracia bez rizikoveho big-bang prepisu

---

## 4) Financny profil projektu

### A) Jednorazova implementacia (CAPEX)
| Oblast | Odhad |
|-------|-------|
| Security + architecture (A-B) | 3 000 - 5 000 EUR |
| Data + backend modernizacia (C-D) | 5 000 - 10 000 EUR |
| Web + mobile delivery (E-F) | 5 000 - 10 000 EUR |
| Infra/ops zavedenie (G) | 2 000 - 3 000 EUR |
| Spolu jednorazovo | 15 000 - 28 000 EUR |

### B) Prevadzka platformy (OPEX)
| Rezim | Odhad |
|------|-------|
| Startup rezim (aktualny sizing) | cca 576 EUR / mesiac |
| Rastovy rezim (cca 5x traffic) | cca 900 - 1 200 EUR / mesiac |

### C) Mobile distribucia
| Polozka | Cena |
|--------|------|
| Expo EAS | 99 USD / mesiac |
| Apple Developer | 99 USD / rok |
| Google Play | 25 USD jednorazovo |

### D) Technicky rozpocet na 12 mesiacov
- Core scenar: 6 912 EUR / rok (len infra pri startup zatazi)
- Rastovy scenar: 10 800 - 14 400 EUR / rok (infra pri vyssom traffic)
- Prvy rok spolu (CAPEX + OPEX, bez miezd):
  - cca 21 912 - 42 400 EUR

Poznamka: Rozpocty vyssie su technicky rozpocet platformy. Neobsahuju komercne naklady firmy (mzdy, sales, marketing, legal, office).

---

## 5) Odhad kapacity a zataze (v cislach)

Predpoklady: API 2x task (1 vCPU/2 GB), web 2x task (0.5 vCPU/1 GB), Redis cache, RDS Multi-AZ.

| Scenar | API throughput | Peak req/min | Subezni online uzivatelia | Odhad MAU |
|-------|----------------|--------------|---------------------------|-----------|
| Bezna prevadzka | 70 - 120 req/s | 4 200 - 7 200 | 120 - 350 | 3 000 - 7 000 |
| Kratka spicka (5-15 min) | 150 - 220 req/s | 9 000 - 13 200 | 350 - 900 | 5 000 - 10 000 |
| Autoscale strop (API do 6 taskov) | 220 - 380 req/s | 13 200 - 22 800 | 900 - 1 800 | 10 000 - 20 000 |

Databazovy orientacny limit (RDS t4g.medium):
- aktivne DB spojenia cez pool: 80 - 120
- trvalo write throughput: 20 - 45 write/s
- trvalo read throughput bez cache: 120 - 250 read/s

CDN orientacne:
- staticky obsah v spicke: 2 000 - 6 000 req/s
- odlahcenie backendu cez cache: 50 - 85% statickych poziadaviek

Poznamka: Finalne kapacity sa potvrdia load testami (k6/Gatling) nad realnymi datami.

---

## 6) Bezpecnost pre investora (co je pokryte)
Projekt pocita s implementaciou klucovych bezpecnostnych vrstiev:
- MFA, RBAC, rate limiting, token rotation
- WAF pravidla, CSP/HSTS, audit trail
- Secrets manager + rotacia klucov
- Security scanning v CI/CD (SAST/SCA)
- Incident runbooky, DR testy, observability dashboard

Business efekt: nizsie riziko bezpecnostnych incidentov a vyssia dovera partnerov aj klientov.

---

## 7) Milniky a casovy plan (8-12 mesiacov)

| Obdobie | Hlavny vysledok |
|--------|------------------|
| Mesiac 1-2 | Security baseline + cielova architektura |
| Mesiac 3-4 | Data refactoring + prve nove API moduly |
| Mesiac 5-6 | Novy web klient + stabilny API core |
| Mesiac 7-9 | Mobilna appka (beta), push, analytics |
| Mesiac 10-12 | Scale-up, rollout, postupne vypinanie legacy casti |

---

## 8) KPI pre investor reporting
- Uptime: > 99.9%
- API p95 latency: <= 350 ms (bezny traffic)
- Crash-free sessions (mobile): >= 99.5%
- Auth failure rate: < 2%
- Deployment frekvencia: tyzdenne release cykly
- Incident MTTR: pod 60 minut pri P1 incidente

---

## 9) Investicny framing (na diskusiu)

| Varianta | Ramcova investicia | Co pokryva |
|---------|---------------------|------------|
| Lean | 80k - 120k EUR | Technicka modernizacia + zakladny produktovy rast |
| Standard | 150k - 250k EUR | Modernizacia + rychlejsi go-to-market + rezerva |
| Growth | 300k+ EUR | Agresivne skalovanie produktu, timu a distribucie |

---

## 10) Closing statement
Essencium ma jasnu cestu od legacy systemu k skalovatelnej platforme s meratelnym dopadom na rast, retenciu a bezpecnost. Technicky plan je pripraveny, rozpocet je vycislitelny a rollout je navrhnuty tak, aby minimalizoval riziko.

Tato investor verzia je samostatny informacny material urceny na predstavenie projektu, financnych potrieb a kapacitneho potencialu.
