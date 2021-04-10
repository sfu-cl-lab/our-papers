load('sun09_groundTruth.mat');

Data = Dtraining;

n = length(Data);
dbox = cell(n, 1);
catgy = cell(n, 1);
sprln = cell(n, 1);

for i = 1:n
    objects = Data(i).annotation.object;
    db = zeros(length(objects), 4);
    for j = 1:length(objects)
        ob = objects(j);
        db(j, :) = [min(ob.polygon.x), max(ob.polygon.x), ...
            min(ob.polygon.y), max(ob.polygon.y)];
    end
    dbox{i} = db;
    [~, catgy{i}] = ismember({objects.name}, validcategories);
    sprln{i} = int8(spatial_exmarr(dbox{i}, dbox{i}));
end

save('data_tr.mat', 'dbox', 'catgy', 'sprln');

Data = Dtest;

n = length(Data);
dbox = cell(n, 1);
catgy = cell(n, 1);
sprln = cell(n, 1);

for i = 1:n
    objects = Data(i).annotation.object;
    db = zeros(length(objects), 4);
    for j = 1:length(objects)
        ob = objects(j);
        db(j, :) = [min(ob.polygon.x), max(ob.polygon.x), ...
            min(ob.polygon.y), max(ob.polygon.y)];
    end
    dbox{i} = db;
    [~, catgy{i}] = ismember({objects.name}, validcategories);
    sprln{i} = int8(spatial_exmarr(dbox{i}, dbox{i}));
end

save('data_ts.mat', 'dbox', 'catgy', 'sprln');
