-- Dev-only seed data. application-dev.yml limits this script to embedded H2.
-- Liquibase currently installs its own Day-1 sample rows first, so replace
-- those rows to keep the dev dataset deterministic at exactly 3 / 5 / 10.

DELETE FROM recon_breaks;
DELETE FROM settlements;
DELETE FROM trades;
DELETE FROM instruments;
DELETE FROM counterparties;

ALTER TABLE counterparties ALTER COLUMN id RESTART WITH 1;
ALTER TABLE instruments ALTER COLUMN id RESTART WITH 1;
ALTER TABLE trades ALTER COLUMN id RESTART WITH 1;

INSERT INTO counterparties (name, lei_code, region) VALUES
  ('Deutsche Bank AG',         '7LTWFZYICNSX8D621K86', 'EMEA'),
  ('Goldman Sachs Group Inc',  '784F5XWPLTWKTBV3E584', 'NAMR'),
  ('Nomura Holdings Inc',      '6N69WMNCQOWKSDLVDX42', 'APAC');

INSERT INTO instruments (symbol, name, asset_class, currency) VALUES
  ('SAP.DE',  'SAP SE',          'EQUITY',       'EUR'),
  ('NVDA',    'NVIDIA Corp',     'EQUITY',       'USD'),
  ('EURUSD',  'EUR/USD spot',    'FX',           'USD'),
  ('BUND10Y', 'German 10Y Bund', 'FIXED_INCOME', 'EUR'),
  ('XAU',     'Gold spot',       'COMMODITY',    'USD');

INSERT INTO trades
    (trade_ref, instrument_id, counterparty_id, quantity, price, trade_date, status, created_at)
VALUES
  ('TR-001', 1, 1,     500.0000,  120.5000, CURRENT_DATE - 5, 'MATCHED',   CURRENT_TIMESTAMP),
  ('TR-002', 2, 2,     100.0000,  890.2500, CURRENT_DATE - 4, 'PENDING',   CURRENT_TIMESTAMP),
  ('TR-003', 3, 1, 1000000.0000,    1.0850, CURRENT_DATE - 3, 'MATCHED',   CURRENT_TIMESTAMP),
  ('TR-004', 4, 3,       5.0000,   99.4000, CURRENT_DATE - 2, 'UNMATCHED', CURRENT_TIMESTAMP),
  ('TR-005', 5, 2,     100.0000, 2150.0000, CURRENT_DATE - 2, 'DISPUTED',  CURRENT_TIMESTAMP),
  ('TR-006', 1, 3,     200.0000,  121.0000, CURRENT_DATE - 1, 'PENDING',   CURRENT_TIMESTAMP),
  ('TR-007', 2, 1,      50.0000,  895.0000, CURRENT_DATE - 1, 'MATCHED',   CURRENT_TIMESTAMP),
  ('TR-008', 3, 2,  500000.0000,    1.0855, CURRENT_DATE,     'PENDING',   CURRENT_TIMESTAMP),
  ('TR-009', 4, 1,      10.0000,   99.5500, CURRENT_DATE,     'MATCHED',   CURRENT_TIMESTAMP),
  ('TR-010', 5, 3,      50.0000, 2148.7500, CURRENT_DATE,     'UNMATCHED', CURRENT_TIMESTAMP);

-- NOTE: recon_breaks demo seed lives only in
-- db/changelog/changes/014-seed-recon-breaks.xml (Liquibase, context="demo",
-- active for the docker profile). It's deliberately NOT duplicated here:
-- this script runs unconditionally under the shared "dev" profile — which
-- @SpringBootTest classes also inherit by default — so any recon_breaks
-- rows added here would contaminate integration tests that assert exact
-- counts (see ReconResultsIntegrationTest / ReconRunIntegrationTest).
