CREATE TABLE water_saving_tips (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(150)  NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

INSERT INTO water_saving_tips (title, content) VALUES
 ('Check taps for leaks', 'A tap that drips once per second wastes over 10,000 litres a year. Check taps, showerheads and flush tanks regularly.'),
 ('Avoid unnecessary water usage', 'Turn the tap off while brushing your teeth or soaping dishes. Small habits add up to many kilolitres a month.'),
 ('Use water-efficient fixtures', 'Aerators, low-flow showerheads and dual-flush cisterns can cut fixture water use by 20-40%.'),
 ('Monitor unusual increases', 'Compare your daily readings. A sudden sustained rise with no change in routine can indicate a hidden leak.'),
 ('Run full loads only', 'Run washing machines and dishwashers only with full loads to save water and electricity.'),
 ('Reuse where safe', 'Water used to rinse vegetables can water plants. Collect it instead of letting it run down the drain.'),
 ('Do a night-time meter test', 'Note your meter before bed and again in the morning with all taps off. Any change suggests a leak.');
