function sp = spatial_exmarr(box, rbox)

nb = size(box, 1);
nrb = size(rbox, 1);

cx = mean(box(:, 1:2), 2);
cy = mean(box(:, 3:4), 2);
cxr = mean(rbox(:, 1:2), 2);
cyr = mean(rbox(:, 3:4), 2);

h = box(:, 4) - box(:, 3);
w = box(:, 2) - box(:, 1);

% construct matrix
abscx = abs(repmat(cx, [1, nrb]) - repmat(cxr', [nb, 1]));
abscy = abs(repmat(cy, [1, nrb]) - repmat(cyr', [nb, 1]));

intw = repmat(w/2, [1, nrb]);
inth = repmat(h/2, [1, nrb]);
extw = repmat(1.5 * w, [1, nrb]);
exth = repmat(1.5 * h, [1, nrb]);

sp = zeros(nb, nrb);

% on-top-of
x_on = abscx <= intw;
y_on = abscy <= inth;
sp(x_on & y_on) = 1;

% above & below
y_next = abscy > inth & abscy <= exth;
y_above = repmat(cy, [1, nrb]) - repmat(cyr', [nb, 1]) >= 0;
sp(y_next & x_on & y_above) = 2; % above
sp(y_next & x_on & ~y_above) = 3; % below

% next-to
x_next = abscx > intw & abscx <= extw;
sp(x_next & y_on) = 4;

% above & below nearby
sp(x_next & y_next & y_above) = 5; % above nearby
sp(x_next & y_next & ~y_above) = 6; % below nearby

% above & below faraway
x_far = abscx > extw;
y_far = abscy > exth;
sp((x_far | y_far) & y_above) = 7; % above faraway
sp((x_far | y_far) & ~y_above) = 8; % below faraway
