ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS original_amount DECIMAL(14,2),
    ADD COLUMN IF NOT EXISTS original_currency VARCHAR(3),
    ADD COLUMN IF NOT EXISTS gateway_amount DECIMAL(14,2),
    ADD COLUMN IF NOT EXISTS gateway_currency VARCHAR(3),
    ADD COLUMN IF NOT EXISTS fx_rate DECIMAL(18,8),
    ADD COLUMN IF NOT EXISTS fx_provider VARCHAR(64),
    ADD COLUMN IF NOT EXISTS fx_quoted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE transaction_items
    ADD COLUMN IF NOT EXISTS price_currency VARCHAR(3);

UPDATE transaction_items ti
SET price_currency = COALESCE(c.currency, 'VND')
FROM courses c
WHERE ti.course_id = c.id
  AND (ti.price_currency IS NULL OR ti.price_currency = '');

UPDATE transaction_items
SET price_currency = 'VND'
WHERE price_currency IS NULL OR price_currency = '';

ALTER TABLE transaction_items
    ALTER COLUMN price_currency SET DEFAULT 'VND';

UPDATE transactions t
SET original_amount = COALESCE(t.original_amount, t.amount),
    original_currency = COALESCE(t.original_currency, item_currency.currency, 'VND'),
    gateway_amount = COALESCE(t.gateway_amount, t.amount),
    gateway_currency = COALESCE(t.gateway_currency, item_currency.currency, 'VND'),
    fx_rate = COALESCE(t.fx_rate, 1),
    fx_provider = COALESCE(t.fx_provider, 'legacy'),
    fx_quoted_at = COALESCE(t.fx_quoted_at, t.created)
FROM (
    SELECT transaction_id, MIN(COALESCE(price_currency, 'VND')) AS currency
    FROM transaction_items
    GROUP BY transaction_id
) item_currency
WHERE item_currency.transaction_id = t.id;
