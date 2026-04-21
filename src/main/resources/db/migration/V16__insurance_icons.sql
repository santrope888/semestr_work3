ALTER TABLE insurances ADD COLUMN icon VARCHAR(50);

UPDATE insurances SET icon = 'bx-shield-alt-2'      WHERE name LIKE 'Базовая%';
UPDATE insurances SET icon = 'bx-lock-alt'          WHERE name = 'От угона';
UPDATE insurances SET icon = 'bx-shield-plus'       WHERE name = 'Полное КАСКО';
UPDATE insurances SET icon = 'bx-shield-quarter'    WHERE name = 'КАСКО Лайт';
UPDATE insurances SET icon = 'bx-car'               WHERE name = 'Защита от ДТП';
UPDATE insurances SET icon = 'bx-group'             WHERE name = 'Страхование пассажиров';
UPDATE insurances SET icon = 'bx-wrench'            WHERE name = 'Помощь на дороге';

UPDATE insurances SET icon = 'bx-shield' WHERE icon IS NULL;