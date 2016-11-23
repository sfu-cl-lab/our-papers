CREATE TABLE Statistics_With_Rank_2007_2008 (
PlayerId INT,
Season TEXT,
SeasonType TEXT,
Goals INT,
GoalsRank INT,
Assists INT,
AssistsRank INT,
Points INT,
PointsRank INT,
PlusMinus INT,
PlusMinusRank INT,
Shots INT,
ShotsRank INT,
Hits INT,
HitsRank INT,
BlockedShots INT,
BlockedShotsRank INT,
Giveaways INT,
GiveawaysRank INT,
Takeaways INT,
TakeawaysRank INT,
PowerPlayTimeOnIce INT,
PPTOIRank INT,
ShortHandedTimeOnIce INT,
SHTOIRank INT,
PenaltyMinutes INT,
PIMRank INT,
TimeOnIce INT,
TOIRank INT
);

ALTER TABLE Statistics_With_Rank_2007_2008 ADD INDEX (PlayerId );

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/statistics_with_rank_2007_2008.csv'
INTO TABLE Statistics_With_Rank_2007_2008
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE Statistics_With_Rank_2008_2009 LIKE Statistics_With_Rank_2007_2008;

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/statistics_with_rank_2008_2009.csv'
INTO TABLE Statistics_With_Rank_2008_2009
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE PlayerValues_With_Rank_2007_2008
(
Season TEXT,
SeasonType TEXT,
PlayerId INT,
PlayerName TEXT,
SumOfSumOfImpact DOUBLE,
ImpactRank INT
);

ALTER TABLE PlayerValues_With_Rank_2007_2008 ADD INDEX (PlayerId );

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/playerseasonaggvalues_2007_2008.csv'
INTO TABLE PlayerValues_With_Rank_2007_2008
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE Player_Values_Statistics_Ranks_2007_2008 AS
SELECT A.*,
B.Goals,
B.GoalsRank, 
B.Assists,
B.AssistsRank, 
B.Points,
B.PointsRank, 
B.PlusMinus,
B.PlusMinusRank, 
B.Shots,
B.ShotsRank, 
B.Hits,
B.HitsRank, 
B.BlockedShots,
B.BlockedShotsRank, 
B.Giveaways,
B.GiveawaysRank, 
B.Takeaways,
B.TakeawaysRank, 
B.PowerPlayTimeOnIce,
B.PPTOIRank, 
B.ShortHandedTimeOnIce,
B.SHTOIRank, 
B.PenaltyMinutes,
B.PIMRank, 
B.TimeOnIce,
B.TOIRank, 
C.Goals AS NextGoals,
C.GoalsRank AS NextGoalsRank,
C.Assists AS NextAssists,
 C.AssistsRank AS NextAssistsRank,
C.Points AS NextPoints,
C.PointsRank AS NextPointsRank,
C.PlusMinus AS NextPlusMinus,
C.PlusMinusRank AS NextPlusMinusRank,
C.Shots AS NextShots,
C.ShotsRank AS NextShotsRank,
C.Hits AS NextHits,
C.HitsRank AS NextHitsRank,
C.BlockedShots AS NextBlockedShots,
C.BlockedShotsRank AS NextBlockedShotsRank,
C.Giveaways AS NextGiveaways,
C.GiveawaysRank AS NextGiveawaysRank,
C.Takeaways AS NextTakeaways,
C.TakeawaysRank AS NextTakeawaysRank,
C.PowerPlayTimeOnIce AS NextPowerPlayTimeOnIce,
C.PPTOIRank AS NextPPTOIRank,
C.ShortHandedTimeOnIce AS NextShortHandedTimeOnIce,
C.SHTOIRank AS NextSHTOIRank,
C.PenaltyMinutes AS NextPenaltyMinutes,
C.PIMRank AS NextPIMRank,
C.TimeOnIce AS NextTimeOnIce,
C.TOIRank AS NextTOIRank,
C.Goals - B.Goals AS ChangeInGoals,
C.GoalsRank - B.GoalsRank AS ChangeInGoalsRank,
C.Assists - B.Assists AS ChangeInAssists,
C.AssistsRank - B.AssistsRank AS ChangeInAssistsRank,
C.Points - B.Points AS ChangeInPoints,
C.PointsRank - B.PointsRank AS ChangeInPointsRank,
C.PlusMinus - B.PlusMinus AS ChangeInPlusMinus,
C.PlusMinusRank - B.PlusMinusRank AS ChangeInPlusMinusRank,
C.Shots - B.Shots AS ChangeInShots,
C.ShotsRank - B.ShotsRank AS ChangeInShotsRank,
C.Hits - B.Hits AS ChangeInHits,
C.HitsRank - B.HitsRank AS ChangeInHitsRank,
C.BlockedShots - B.BlockedShots AS ChangeInBlockedShots,
C.BlockedShotsRank - B.BlockedShotsRank AS ChangeInBlockedShotsRank,
C.Giveaways - B.Giveaways AS ChangeInGiveaways,
C.GiveawaysRank - B.GiveawaysRank AS ChangeInGiveawaysRank,
C.Takeaways - B.Takeaways AS ChangeInTakeaways,
C.TakeawaysRank - B.TakeawaysRank AS ChangeInTakeawaysRank,
C.PowerPlayTimeOnIce - B.PowerPlayTimeOnIce AS ChangeInPPTOI,
C.PPTOIRank - B.PPTOIRank AS ChangeInPPTOIRank,
C.ShortHandedTimeOnIce - B.ShortHandedTimeOnIce AS ChangeInSHTOI,
C.SHTOIRank - B.SHTOIRank AS ChangeInSHTOIRank,
C.PenaltyMinutes - B.PenaltyMinutes AS ChangeInPIM,
C.PIMRank - B.PIMRank AS ChangeInPIMRank,
C.TimeOnIce - B.TimeOnIce AS ChangeInTOI,
C.TOIRank - B.TOIRank AS ChangeInTOIRank
FROM playervalues_with_rank_2007_2008 AS A,
statistics_with_rank_2007_2008 AS B,
statistics_with_rank_2008_2009 AS C
WHERE A.PlayerId = B.PlayerId
AND A.PlayerId = C.PlayerId;


CREATE TABLE Statistics_With_Rank_2009_2010 LIKE Statistics_With_Rank_2007_2008;

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/statistics_with_rank_2009_2010.csv'
INTO TABLE Statistics_With_Rank_2009_2010
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE PlayerValues_With_Rank_2008_2009
(
Season TEXT,
SeasonType TEXT,
PlayerId INT,
PlayerName TEXT,
SumOfSumOfImpact DOUBLE,
ImpactRank INT
);

ALTER TABLE PlayerValues_With_Rank_2008_2009 ADD INDEX (PlayerId );

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/playerseasonaggvalues_2008_2009.csv'
INTO TABLE PlayerValues_With_Rank_2008_2009
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE Player_Values_Statistics_Ranks_2008_2009 AS
SELECT A.*,
B.Goals,
B.GoalsRank, 
B.Assists,
B.AssistsRank, 
B.Points,
B.PointsRank, 
B.PlusMinus,
B.PlusMinusRank, 
B.Shots,
B.ShotsRank, 
B.Hits,
B.HitsRank, 
B.BlockedShots,
B.BlockedShotsRank, 
B.Giveaways,
B.GiveawaysRank, 
B.Takeaways,
B.TakeawaysRank, 
B.PowerPlayTimeOnIce,
B.PPTOIRank, 
B.ShortHandedTimeOnIce,
B.SHTOIRank, 
B.PenaltyMinutes,
B.PIMRank, 
B.TimeOnIce,
B.TOIRank, 
C.Goals AS NextGoals,
C.GoalsRank AS NextGoalsRank,
C.Assists AS NextAssists,
 C.AssistsRank AS NextAssistsRank,
C.Points AS NextPoints,
C.PointsRank AS NextPointsRank,
C.PlusMinus AS NextPlusMinus,
C.PlusMinusRank AS NextPlusMinusRank,
C.Shots AS NextShots,
C.ShotsRank AS NextShotsRank,
C.Hits AS NextHits,
C.HitsRank AS NextHitsRank,
C.BlockedShots AS NextBlockedShots,
C.BlockedShotsRank AS NextBlockedShotsRank,
C.Giveaways AS NextGiveaways,
C.GiveawaysRank AS NextGiveawaysRank,
C.Takeaways AS NextTakeaways,
C.TakeawaysRank AS NextTakeawaysRank,
C.PowerPlayTimeOnIce AS NextPowerPlayTimeOnIce,
C.PPTOIRank AS NextPPTOIRank,
C.ShortHandedTimeOnIce AS NextShortHandedTimeOnIce,
C.SHTOIRank AS NextSHTOIRank,
C.PenaltyMinutes AS NextPenaltyMinutes,
C.PIMRank AS NextPIMRank,
C.TimeOnIce AS NextTimeOnIce,
C.TOIRank AS NextTOIRank,
C.Goals - B.Goals AS ChangeInGoals,
C.GoalsRank - B.GoalsRank AS ChangeInGoalsRank,
C.Assists - B.Assists AS ChangeInAssists,
C.AssistsRank - B.AssistsRank AS ChangeInAssistsRank,
C.Points - B.Points AS ChangeInPoints,
C.PointsRank - B.PointsRank AS ChangeInPointsRank,
C.PlusMinus - B.PlusMinus AS ChangeInPlusMinus,
C.PlusMinusRank - B.PlusMinusRank AS ChangeInPlusMinusRank,
C.Shots - B.Shots AS ChangeInShots,
C.ShotsRank - B.ShotsRank AS ChangeInShotsRank,
C.Hits - B.Hits AS ChangeInHits,
C.HitsRank - B.HitsRank AS ChangeInHitsRank,
C.BlockedShots - B.BlockedShots AS ChangeInBlockedShots,
C.BlockedShotsRank - B.BlockedShotsRank AS ChangeInBlockedShotsRank,
C.Giveaways - B.Giveaways AS ChangeInGiveaways,
C.GiveawaysRank - B.GiveawaysRank AS ChangeInGiveawaysRank,
C.Takeaways - B.Takeaways AS ChangeInTakeaways,
C.TakeawaysRank - B.TakeawaysRank AS ChangeInTakeawaysRank,
C.PowerPlayTimeOnIce - B.PowerPlayTimeOnIce AS ChangeInPPTOI,
C.PPTOIRank - B.PPTOIRank AS ChangeInPPTOIRank,
C.ShortHandedTimeOnIce - B.ShortHandedTimeOnIce AS ChangeInSHTOI,
C.SHTOIRank - B.SHTOIRank AS ChangeInSHTOIRank,
C.PenaltyMinutes - B.PenaltyMinutes AS ChangeInPIM,
C.PIMRank - B.PIMRank AS ChangeInPIMRank,
C.TimeOnIce - B.TimeOnIce AS ChangeInTOI,
C.TOIRank - B.TOIRank AS ChangeInTOIRank
FROM playervalues_with_rank_2008_2009 AS A,
statistics_with_rank_2008_2009 AS B,
statistics_with_rank_2009_2010 AS C
WHERE A.PlayerId = B.PlayerId
AND A.PlayerId = C.PlayerId;

#2009-2010


CREATE TABLE Statistics_With_Rank_2010_2011 LIKE Statistics_With_Rank_2007_2008;

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/statistics_with_rank_2010_2011.csv'
INTO TABLE Statistics_With_Rank_2010_2011
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE PlayerValues_With_Rank_2009_2010
(
Season TEXT,
SeasonType TEXT,
PlayerId INT,
PlayerName TEXT,
SumOfSumOfImpact DOUBLE,
ImpactRank INT
);

ALTER TABLE PlayerValues_With_Rank_2009_2010 ADD INDEX (PlayerId );

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/playerseasonaggvalues_2009_2010.csv'
INTO TABLE PlayerValues_With_Rank_2009_2010
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE Player_Values_Statistics_Ranks_2009_2010 AS
SELECT A.*,
B.Goals,
B.GoalsRank, 
B.Assists,
B.AssistsRank, 
B.Points,
B.PointsRank, 
B.PlusMinus,
B.PlusMinusRank, 
B.Shots,
B.ShotsRank, 
B.Hits,
B.HitsRank, 
B.BlockedShots,
B.BlockedShotsRank, 
B.Giveaways,
B.GiveawaysRank, 
B.Takeaways,
B.TakeawaysRank, 
B.PowerPlayTimeOnIce,
B.PPTOIRank, 
B.ShortHandedTimeOnIce,
B.SHTOIRank, 
B.PenaltyMinutes,
B.PIMRank, 
B.TimeOnIce,
B.TOIRank, 
C.Goals AS NextGoals,
C.GoalsRank AS NextGoalsRank,
C.Assists AS NextAssists,
 C.AssistsRank AS NextAssistsRank,
C.Points AS NextPoints,
C.PointsRank AS NextPointsRank,
C.PlusMinus AS NextPlusMinus,
C.PlusMinusRank AS NextPlusMinusRank,
C.Shots AS NextShots,
C.ShotsRank AS NextShotsRank,
C.Hits AS NextHits,
C.HitsRank AS NextHitsRank,
C.BlockedShots AS NextBlockedShots,
C.BlockedShotsRank AS NextBlockedShotsRank,
C.Giveaways AS NextGiveaways,
C.GiveawaysRank AS NextGiveawaysRank,
C.Takeaways AS NextTakeaways,
C.TakeawaysRank AS NextTakeawaysRank,
C.PowerPlayTimeOnIce AS NextPowerPlayTimeOnIce,
C.PPTOIRank AS NextPPTOIRank,
C.ShortHandedTimeOnIce AS NextShortHandedTimeOnIce,
C.SHTOIRank AS NextSHTOIRank,
C.PenaltyMinutes AS NextPenaltyMinutes,
C.PIMRank AS NextPIMRank,
C.TimeOnIce AS NextTimeOnIce,
C.TOIRank AS NextTOIRank,
C.Goals - B.Goals AS ChangeInGoals,
C.GoalsRank - B.GoalsRank AS ChangeInGoalsRank,
C.Assists - B.Assists AS ChangeInAssists,
C.AssistsRank - B.AssistsRank AS ChangeInAssistsRank,
C.Points - B.Points AS ChangeInPoints,
C.PointsRank - B.PointsRank AS ChangeInPointsRank,
C.PlusMinus - B.PlusMinus AS ChangeInPlusMinus,
C.PlusMinusRank - B.PlusMinusRank AS ChangeInPlusMinusRank,
C.Shots - B.Shots AS ChangeInShots,
C.ShotsRank - B.ShotsRank AS ChangeInShotsRank,
C.Hits - B.Hits AS ChangeInHits,
C.HitsRank - B.HitsRank AS ChangeInHitsRank,
C.BlockedShots - B.BlockedShots AS ChangeInBlockedShots,
C.BlockedShotsRank - B.BlockedShotsRank AS ChangeInBlockedShotsRank,
C.Giveaways - B.Giveaways AS ChangeInGiveaways,
C.GiveawaysRank - B.GiveawaysRank AS ChangeInGiveawaysRank,
C.Takeaways - B.Takeaways AS ChangeInTakeaways,
C.TakeawaysRank - B.TakeawaysRank AS ChangeInTakeawaysRank,
C.PowerPlayTimeOnIce - B.PowerPlayTimeOnIce AS ChangeInPPTOI,
C.PPTOIRank - B.PPTOIRank AS ChangeInPPTOIRank,
C.ShortHandedTimeOnIce - B.ShortHandedTimeOnIce AS ChangeInSHTOI,
C.SHTOIRank - B.SHTOIRank AS ChangeInSHTOIRank,
C.PenaltyMinutes - B.PenaltyMinutes AS ChangeInPIM,
C.PIMRank - B.PIMRank AS ChangeInPIMRank,
C.TimeOnIce - B.TimeOnIce AS ChangeInTOI,
C.TOIRank - B.TOIRank AS ChangeInTOIRank
FROM playervalues_with_rank_2009_2010 AS A,
statistics_with_rank_2009_2010 AS B,
statistics_with_rank_2010_2011 AS C
WHERE A.PlayerId = B.PlayerId
AND A.PlayerId = C.PlayerId;


#2010-2011

CREATE TABLE Statistics_With_Rank_2011_2012 LIKE Statistics_With_Rank_2007_2008;

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/statistics_with_rank_2011_2012.csv'
INTO TABLE Statistics_With_Rank_2011_2012
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE PlayerValues_With_Rank_2010_2011
(
Season TEXT,
SeasonType TEXT,
PlayerId INT,
PlayerName TEXT,
SumOfSumOfImpact DOUBLE,
ImpactRank INT
);

ALTER TABLE PlayerValues_With_Rank_2010_2011 ADD INDEX (PlayerId );

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/playerseasonaggvalues_2010_2011.csv'
INTO TABLE PlayerValues_With_Rank_2010_2011
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE Player_Values_Statistics_Ranks_2010_2011 AS
SELECT A.*,
B.Goals,
B.GoalsRank, 
B.Assists,
B.AssistsRank, 
B.Points,
B.PointsRank, 
B.PlusMinus,
B.PlusMinusRank, 
B.Shots,
B.ShotsRank, 
B.Hits,
B.HitsRank, 
B.BlockedShots,
B.BlockedShotsRank, 
B.Giveaways,
B.GiveawaysRank, 
B.Takeaways,
B.TakeawaysRank, 
B.PowerPlayTimeOnIce,
B.PPTOIRank, 
B.ShortHandedTimeOnIce,
B.SHTOIRank, 
B.PenaltyMinutes,
B.PIMRank, 
B.TimeOnIce,
B.TOIRank, 
C.Goals AS NextGoals,
C.GoalsRank AS NextGoalsRank,
C.Assists AS NextAssists,
 C.AssistsRank AS NextAssistsRank,
C.Points AS NextPoints,
C.PointsRank AS NextPointsRank,
C.PlusMinus AS NextPlusMinus,
C.PlusMinusRank AS NextPlusMinusRank,
C.Shots AS NextShots,
C.ShotsRank AS NextShotsRank,
C.Hits AS NextHits,
C.HitsRank AS NextHitsRank,
C.BlockedShots AS NextBlockedShots,
C.BlockedShotsRank AS NextBlockedShotsRank,
C.Giveaways AS NextGiveaways,
C.GiveawaysRank AS NextGiveawaysRank,
C.Takeaways AS NextTakeaways,
C.TakeawaysRank AS NextTakeawaysRank,
C.PowerPlayTimeOnIce AS NextPowerPlayTimeOnIce,
C.PPTOIRank AS NextPPTOIRank,
C.ShortHandedTimeOnIce AS NextShortHandedTimeOnIce,
C.SHTOIRank AS NextSHTOIRank,
C.PenaltyMinutes AS NextPenaltyMinutes,
C.PIMRank AS NextPIMRank,
C.TimeOnIce AS NextTimeOnIce,
C.TOIRank AS NextTOIRank,
C.Goals - B.Goals AS ChangeInGoals,
C.GoalsRank - B.GoalsRank AS ChangeInGoalsRank,
C.Assists - B.Assists AS ChangeInAssists,
C.AssistsRank - B.AssistsRank AS ChangeInAssistsRank,
C.Points - B.Points AS ChangeInPoints,
C.PointsRank - B.PointsRank AS ChangeInPointsRank,
C.PlusMinus - B.PlusMinus AS ChangeInPlusMinus,
C.PlusMinusRank - B.PlusMinusRank AS ChangeInPlusMinusRank,
C.Shots - B.Shots AS ChangeInShots,
C.ShotsRank - B.ShotsRank AS ChangeInShotsRank,
C.Hits - B.Hits AS ChangeInHits,
C.HitsRank - B.HitsRank AS ChangeInHitsRank,
C.BlockedShots - B.BlockedShots AS ChangeInBlockedShots,
C.BlockedShotsRank - B.BlockedShotsRank AS ChangeInBlockedShotsRank,
C.Giveaways - B.Giveaways AS ChangeInGiveaways,
C.GiveawaysRank - B.GiveawaysRank AS ChangeInGiveawaysRank,
C.Takeaways - B.Takeaways AS ChangeInTakeaways,
C.TakeawaysRank - B.TakeawaysRank AS ChangeInTakeawaysRank,
C.PowerPlayTimeOnIce - B.PowerPlayTimeOnIce AS ChangeInPPTOI,
C.PPTOIRank - B.PPTOIRank AS ChangeInPPTOIRank,
C.ShortHandedTimeOnIce - B.ShortHandedTimeOnIce AS ChangeInSHTOI,
C.SHTOIRank - B.SHTOIRank AS ChangeInSHTOIRank,
C.PenaltyMinutes - B.PenaltyMinutes AS ChangeInPIM,
C.PIMRank - B.PIMRank AS ChangeInPIMRank,
C.TimeOnIce - B.TimeOnIce AS ChangeInTOI,
C.TOIRank - B.TOIRank AS ChangeInTOIRank
FROM playervalues_with_rank_2010_2011 AS A,
statistics_with_rank_2010_2011 AS B,
statistics_with_rank_2011_2012 AS C
WHERE A.PlayerId = B.PlayerId
AND A.PlayerId = C.PlayerId;


#2011-2012

CREATE TABLE Statistics_With_Rank_2012_2013 LIKE Statistics_With_Rank_2007_2008;

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/statistics_with_rank_2012_2013.csv'
INTO TABLE Statistics_With_Rank_2012_2013
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE PlayerValues_With_Rank_2011_2012
(
Season TEXT,
SeasonType TEXT,
PlayerId INT,
PlayerName TEXT,
SumOfSumOfImpact DOUBLE,
ImpactRank INT
);

ALTER TABLE PlayerValues_With_Rank_2011_2012 ADD INDEX (PlayerId );

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/playerseasonaggvalues_2011_2012.csv'
INTO TABLE PlayerValues_With_Rank_2011_2012
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE Player_Values_Statistics_Ranks_2011_2012 AS
SELECT A.*,
B.Goals,
B.GoalsRank, 
B.Assists,
B.AssistsRank, 
B.Points,
B.PointsRank, 
B.PlusMinus,
B.PlusMinusRank, 
B.Shots,
B.ShotsRank, 
B.Hits,
B.HitsRank, 
B.BlockedShots,
B.BlockedShotsRank, 
B.Giveaways,
B.GiveawaysRank, 
B.Takeaways,
B.TakeawaysRank, 
B.PowerPlayTimeOnIce,
B.PPTOIRank, 
B.ShortHandedTimeOnIce,
B.SHTOIRank, 
B.PenaltyMinutes,
B.PIMRank, 
B.TimeOnIce,
B.TOIRank, 
C.Goals AS NextGoals,
C.GoalsRank AS NextGoalsRank,
C.Assists AS NextAssists,
 C.AssistsRank AS NextAssistsRank,
C.Points AS NextPoints,
C.PointsRank AS NextPointsRank,
C.PlusMinus AS NextPlusMinus,
C.PlusMinusRank AS NextPlusMinusRank,
C.Shots AS NextShots,
C.ShotsRank AS NextShotsRank,
C.Hits AS NextHits,
C.HitsRank AS NextHitsRank,
C.BlockedShots AS NextBlockedShots,
C.BlockedShotsRank AS NextBlockedShotsRank,
C.Giveaways AS NextGiveaways,
C.GiveawaysRank AS NextGiveawaysRank,
C.Takeaways AS NextTakeaways,
C.TakeawaysRank AS NextTakeawaysRank,
C.PowerPlayTimeOnIce AS NextPowerPlayTimeOnIce,
C.PPTOIRank AS NextPPTOIRank,
C.ShortHandedTimeOnIce AS NextShortHandedTimeOnIce,
C.SHTOIRank AS NextSHTOIRank,
C.PenaltyMinutes AS NextPenaltyMinutes,
C.PIMRank AS NextPIMRank,
C.TimeOnIce AS NextTimeOnIce,
C.TOIRank AS NextTOIRank,
C.Goals - B.Goals AS ChangeInGoals,
C.GoalsRank - B.GoalsRank AS ChangeInGoalsRank,
C.Assists - B.Assists AS ChangeInAssists,
C.AssistsRank - B.AssistsRank AS ChangeInAssistsRank,
C.Points - B.Points AS ChangeInPoints,
C.PointsRank - B.PointsRank AS ChangeInPointsRank,
C.PlusMinus - B.PlusMinus AS ChangeInPlusMinus,
C.PlusMinusRank - B.PlusMinusRank AS ChangeInPlusMinusRank,
C.Shots - B.Shots AS ChangeInShots,
C.ShotsRank - B.ShotsRank AS ChangeInShotsRank,
C.Hits - B.Hits AS ChangeInHits,
C.HitsRank - B.HitsRank AS ChangeInHitsRank,
C.BlockedShots - B.BlockedShots AS ChangeInBlockedShots,
C.BlockedShotsRank - B.BlockedShotsRank AS ChangeInBlockedShotsRank,
C.Giveaways - B.Giveaways AS ChangeInGiveaways,
C.GiveawaysRank - B.GiveawaysRank AS ChangeInGiveawaysRank,
C.Takeaways - B.Takeaways AS ChangeInTakeaways,
C.TakeawaysRank - B.TakeawaysRank AS ChangeInTakeawaysRank,
C.PowerPlayTimeOnIce - B.PowerPlayTimeOnIce AS ChangeInPPTOI,
C.PPTOIRank - B.PPTOIRank AS ChangeInPPTOIRank,
C.ShortHandedTimeOnIce - B.ShortHandedTimeOnIce AS ChangeInSHTOI,
C.SHTOIRank - B.SHTOIRank AS ChangeInSHTOIRank,
C.PenaltyMinutes - B.PenaltyMinutes AS ChangeInPIM,
C.PIMRank - B.PIMRank AS ChangeInPIMRank,
C.TimeOnIce - B.TimeOnIce AS ChangeInTOI,
C.TOIRank - B.TOIRank AS ChangeInTOIRank
FROM playervalues_with_rank_2011_2012 AS A,
statistics_with_rank_2011_2012 AS B,
statistics_with_rank_2012_2013 AS C
WHERE A.PlayerId = B.PlayerId
AND A.PlayerId = C.PlayerId;


#2012-2013

CREATE TABLE Statistics_With_Rank_2013_2014 LIKE Statistics_With_Rank_2007_2008;

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/statistics_with_rank_2013_2014.csv'
INTO TABLE Statistics_With_Rank_2013_2014
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE PlayerValues_With_Rank_2012_2013
(
Season TEXT,
SeasonType TEXT,
PlayerId INT,
PlayerName TEXT,
SumOfSumOfImpact DOUBLE,
ImpactRank INT
);

ALTER TABLE PlayerValues_With_Rank_2012_2013 ADD INDEX (PlayerId );

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/playerseasonaggvalues_2012_2013.csv'
INTO TABLE PlayerValues_With_Rank_2012_2013
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE Player_Values_Statistics_Ranks_2012_2013 AS
SELECT A.*,
B.Goals,
B.GoalsRank, 
B.Assists,
B.AssistsRank, 
B.Points,
B.PointsRank, 
B.PlusMinus,
B.PlusMinusRank, 
B.Shots,
B.ShotsRank, 
B.Hits,
B.HitsRank, 
B.BlockedShots,
B.BlockedShotsRank, 
B.Giveaways,
B.GiveawaysRank, 
B.Takeaways,
B.TakeawaysRank, 
B.PowerPlayTimeOnIce,
B.PPTOIRank, 
B.ShortHandedTimeOnIce,
B.SHTOIRank, 
B.PenaltyMinutes,
B.PIMRank, 
B.TimeOnIce,
B.TOIRank, 
C.Goals AS NextGoals,
C.GoalsRank AS NextGoalsRank,
C.Assists AS NextAssists,
 C.AssistsRank AS NextAssistsRank,
C.Points AS NextPoints,
C.PointsRank AS NextPointsRank,
C.PlusMinus AS NextPlusMinus,
C.PlusMinusRank AS NextPlusMinusRank,
C.Shots AS NextShots,
C.ShotsRank AS NextShotsRank,
C.Hits AS NextHits,
C.HitsRank AS NextHitsRank,
C.BlockedShots AS NextBlockedShots,
C.BlockedShotsRank AS NextBlockedShotsRank,
C.Giveaways AS NextGiveaways,
C.GiveawaysRank AS NextGiveawaysRank,
C.Takeaways AS NextTakeaways,
C.TakeawaysRank AS NextTakeawaysRank,
C.PowerPlayTimeOnIce AS NextPowerPlayTimeOnIce,
C.PPTOIRank AS NextPPTOIRank,
C.ShortHandedTimeOnIce AS NextShortHandedTimeOnIce,
C.SHTOIRank AS NextSHTOIRank,
C.PenaltyMinutes AS NextPenaltyMinutes,
C.PIMRank AS NextPIMRank,
C.TimeOnIce AS NextTimeOnIce,
C.TOIRank AS NextTOIRank,
C.Goals - B.Goals AS ChangeInGoals,
C.GoalsRank - B.GoalsRank AS ChangeInGoalsRank,
C.Assists - B.Assists AS ChangeInAssists,
C.AssistsRank - B.AssistsRank AS ChangeInAssistsRank,
C.Points - B.Points AS ChangeInPoints,
C.PointsRank - B.PointsRank AS ChangeInPointsRank,
C.PlusMinus - B.PlusMinus AS ChangeInPlusMinus,
C.PlusMinusRank - B.PlusMinusRank AS ChangeInPlusMinusRank,
C.Shots - B.Shots AS ChangeInShots,
C.ShotsRank - B.ShotsRank AS ChangeInShotsRank,
C.Hits - B.Hits AS ChangeInHits,
C.HitsRank - B.HitsRank AS ChangeInHitsRank,
C.BlockedShots - B.BlockedShots AS ChangeInBlockedShots,
C.BlockedShotsRank - B.BlockedShotsRank AS ChangeInBlockedShotsRank,
C.Giveaways - B.Giveaways AS ChangeInGiveaways,
C.GiveawaysRank - B.GiveawaysRank AS ChangeInGiveawaysRank,
C.Takeaways - B.Takeaways AS ChangeInTakeaways,
C.TakeawaysRank - B.TakeawaysRank AS ChangeInTakeawaysRank,
C.PowerPlayTimeOnIce - B.PowerPlayTimeOnIce AS ChangeInPPTOI,
C.PPTOIRank - B.PPTOIRank AS ChangeInPPTOIRank,
C.ShortHandedTimeOnIce - B.ShortHandedTimeOnIce AS ChangeInSHTOI,
C.SHTOIRank - B.SHTOIRank AS ChangeInSHTOIRank,
C.PenaltyMinutes - B.PenaltyMinutes AS ChangeInPIM,
C.PIMRank - B.PIMRank AS ChangeInPIMRank,
C.TimeOnIce - B.TimeOnIce AS ChangeInTOI,
C.TOIRank - B.TOIRank AS ChangeInTOIRank
FROM playervalues_with_rank_2012_2013 AS A,
statistics_with_rank_2012_2013 AS B,
statistics_with_rank_2013_2014 AS C
WHERE A.PlayerId = B.PlayerId
AND A.PlayerId = C.PlayerId;


#2013-2014

CREATE TABLE Statistics_With_Rank_2014_2015 LIKE Statistics_With_Rank_2007_2008;

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/statistics_with_rank_2014_2015.csv'
INTO TABLE Statistics_With_Rank_2014_2015
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE PlayerValues_With_Rank_2013_2014
(
Season TEXT,
SeasonType TEXT,
PlayerId INT,
PlayerName TEXT,
SumOfSumOfImpact DOUBLE,
ImpactRank INT
);

ALTER TABLE PlayerValues_With_Rank_2013_2014 ADD INDEX (PlayerId );

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/playerseasonaggvalues_2013_2014.csv'
INTO TABLE PlayerValues_With_Rank_2013_2014
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE Player_Values_Statistics_Ranks_2013_2014 AS
SELECT A.*,
B.Goals,
B.GoalsRank, 
B.Assists,
B.AssistsRank, 
B.Points,
B.PointsRank, 
B.PlusMinus,
B.PlusMinusRank, 
B.Shots,
B.ShotsRank, 
B.Hits,
B.HitsRank, 
B.BlockedShots,
B.BlockedShotsRank, 
B.Giveaways,
B.GiveawaysRank, 
B.Takeaways,
B.TakeawaysRank, 
B.PowerPlayTimeOnIce,
B.PPTOIRank, 
B.ShortHandedTimeOnIce,
B.SHTOIRank, 
B.PenaltyMinutes,
B.PIMRank, 
B.TimeOnIce,
B.TOIRank, 
C.Goals AS NextGoals,
C.GoalsRank AS NextGoalsRank,
C.Assists AS NextAssists,
 C.AssistsRank AS NextAssistsRank,
C.Points AS NextPoints,
C.PointsRank AS NextPointsRank,
C.PlusMinus AS NextPlusMinus,
C.PlusMinusRank AS NextPlusMinusRank,
C.Shots AS NextShots,
C.ShotsRank AS NextShotsRank,
C.Hits AS NextHits,
C.HitsRank AS NextHitsRank,
C.BlockedShots AS NextBlockedShots,
C.BlockedShotsRank AS NextBlockedShotsRank,
C.Giveaways AS NextGiveaways,
C.GiveawaysRank AS NextGiveawaysRank,
C.Takeaways AS NextTakeaways,
C.TakeawaysRank AS NextTakeawaysRank,
C.PowerPlayTimeOnIce AS NextPowerPlayTimeOnIce,
C.PPTOIRank AS NextPPTOIRank,
C.ShortHandedTimeOnIce AS NextShortHandedTimeOnIce,
C.SHTOIRank AS NextSHTOIRank,
C.PenaltyMinutes AS NextPenaltyMinutes,
C.PIMRank AS NextPIMRank,
C.TimeOnIce AS NextTimeOnIce,
C.TOIRank AS NextTOIRank,
C.Goals - B.Goals AS ChangeInGoals,
C.GoalsRank - B.GoalsRank AS ChangeInGoalsRank,
C.Assists - B.Assists AS ChangeInAssists,
C.AssistsRank - B.AssistsRank AS ChangeInAssistsRank,
C.Points - B.Points AS ChangeInPoints,
C.PointsRank - B.PointsRank AS ChangeInPointsRank,
C.PlusMinus - B.PlusMinus AS ChangeInPlusMinus,
C.PlusMinusRank - B.PlusMinusRank AS ChangeInPlusMinusRank,
C.Shots - B.Shots AS ChangeInShots,
C.ShotsRank - B.ShotsRank AS ChangeInShotsRank,
C.Hits - B.Hits AS ChangeInHits,
C.HitsRank - B.HitsRank AS ChangeInHitsRank,
C.BlockedShots - B.BlockedShots AS ChangeInBlockedShots,
C.BlockedShotsRank - B.BlockedShotsRank AS ChangeInBlockedShotsRank,
C.Giveaways - B.Giveaways AS ChangeInGiveaways,
C.GiveawaysRank - B.GiveawaysRank AS ChangeInGiveawaysRank,
C.Takeaways - B.Takeaways AS ChangeInTakeaways,
C.TakeawaysRank - B.TakeawaysRank AS ChangeInTakeawaysRank,
C.PowerPlayTimeOnIce - B.PowerPlayTimeOnIce AS ChangeInPPTOI,
C.PPTOIRank - B.PPTOIRank AS ChangeInPPTOIRank,
C.ShortHandedTimeOnIce - B.ShortHandedTimeOnIce AS ChangeInSHTOI,
C.SHTOIRank - B.SHTOIRank AS ChangeInSHTOIRank,
C.PenaltyMinutes - B.PenaltyMinutes AS ChangeInPIM,
C.PIMRank - B.PIMRank AS ChangeInPIMRank,
C.TimeOnIce - B.TimeOnIce AS ChangeInTOI,
C.TOIRank - B.TOIRank AS ChangeInTOIRank
FROM playervalues_with_rank_2013_2014 AS A,
statistics_with_rank_2013_2014 AS B,
statistics_with_rank_2014_2015 AS C
WHERE A.PlayerId = B.PlayerId
AND A.PlayerId = C.PlayerId;


#2014-2015

CREATE TABLE PlayerValues_With_Rank_2014_2015
(
Season TEXT,
SeasonType TEXT,
PlayerId INT,
PlayerName TEXT,
SumOfSumOfImpact DOUBLE,
ImpactRank INT
);

ALTER TABLE PlayerValues_With_Rank_2014_2015 ADD INDEX (PlayerId );

LOAD DATA LOCAL INFILE 'D:/Kurt\'s Work/Dropbox/UAI/figures-results/playerseasonaggvalues_2014_2015.csv'
INTO TABLE PlayerValues_With_Rank_2014_2015
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

CREATE TABLE Player_Values_Statistics_Ranks_2014_2015 AS
SELECT A.*,
B.Goals,
B.GoalsRank, 
B.Assists,
B.AssistsRank, 
B.Points,
B.PointsRank, 
B.PlusMinus,
B.PlusMinusRank, 
B.Shots,
B.ShotsRank, 
B.Hits,
B.HitsRank, 
B.BlockedShots,
B.BlockedShotsRank, 
B.Giveaways,
B.GiveawaysRank, 
B.Takeaways,
B.TakeawaysRank, 
B.PowerPlayTimeOnIce,
B.PPTOIRank, 
B.ShortHandedTimeOnIce,
B.SHTOIRank, 
B.PenaltyMinutes,
B.PIMRank, 
B.TimeOnIce,
B.TOIRank
FROM playervalues_with_rank_2014_2015 AS A,
statistics_with_rank_2014_2015 AS B
WHERE A.PlayerId = B.PlayerId;
