insert into User select * from MovieLens_std.User order by RAND() limit 100;

insert into u2base 
select * from MovieLens_std.u2base 
where user_id in (select user_id from MovieLens_Small.User);

insert into item2
select * from MovieLens_std.item2 
where item_id in (select item_id from MovieLens_Small.u2base);

select distinct user_id from u2base; -- 100
select distinct item_id from item2;  -- 1128
select distinct item_id,user_id, rating from u2base;  -- 7526

-- total tuples: 8754

select count(*),rating from u2base group by rating; 

select count(*),item_id from u2base group by item_id; 

select count(*),user_id from u2base group by user_id; 


----  MovieLens_Small_Training1
CREATE TABLE `User` (
	`user_id` INT(11) NOT NULL DEFAULT '0',
	`Age` VARCHAR(45) NULL DEFAULT NULL,
	`Gender` VARCHAR(5) NULL DEFAULT NULL,
	PRIMARY KEY (`user_id`),
	INDEX `User_Age` (`Age`),
	INDEX `User_Gender` (`Gender`)
)
COMMENT='select 80 users from MovieLens_std, March 25 2015'
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
ROW_FORMAT=FIXED;

----  MovieLens_Small_Test1
CREATE TABLE `User` (
	`user_id` INT(11) NOT NULL DEFAULT '0',
	`Age` VARCHAR(45) NULL DEFAULT NULL,
	`Gender` VARCHAR(5) NULL DEFAULT NULL,
	PRIMARY KEY (`user_id`),
	INDEX `User_Age` (`Age`),
	INDEX `User_Gender` (`Gender`)
)
COMMENT='select 20 users from MovieLens_Small, March 25 2015'
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
ROW_FORMAT=FIXED;

CREATE TABLE `item2` (
	`item_id` INT(11) NOT NULL DEFAULT '0',
	`action` VARCHAR(4) NULL DEFAULT NULL,
	`drama` VARCHAR(4) NULL DEFAULT NULL,
	`horror` VARCHAR(4) NULL DEFAULT NULL,
	PRIMARY KEY (`item_id`),
	INDEX `MovieLens_action` (`action`),
	INDEX `MovieLens_drama` (`drama`),
	INDEX `MovieLens_horrow` (`horror`)
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
ROW_FORMAT=FIXED;


CREATE TABLE `u2base` (
	`user_id` INT(11) NOT NULL DEFAULT '0',
	`item_id` INT(11) NOT NULL DEFAULT '0',
	`rating` VARCHAR(45) NULL DEFAULT NULL,
	PRIMARY KEY (`user_id`, `item_id`),
	INDEX `FK_u2base_1` (`item_id`),
	INDEX `FK_u2base_2` (`user_id`),
	INDEX `u2base_rating` (`rating`),
	CONSTRAINT `FK_u2base_User` FOREIGN KEY (`user_id`) REFERENCES `User` (`user_id`) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT `FK_u2base_item2` FOREIGN KEY (`item_id`) REFERENCES `item2` (`item_id`) ON UPDATE CASCADE ON DELETE CASCADE
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
ROW_FORMAT=DYNAMIC;


use MovieLens_Small_Training1;


insert into User select * from MovieLens_Small.User order by RAND() limit 80;

insert into u2base 
select * from MovieLens_Small.u2base 
where user_id in (select user_id from MovieLens_Small_Training1.User);

insert into item2
select * from MovieLens_Small.item2 
where item_id in (select item_id from MovieLens_Small_Training1.u2base);

select distinct user_id from u2base; -- 80
select distinct item_id from item2;  -- 1100
select distinct item_id,user_id, rating from u2base;  -- 6289


use MovieLens_Small_Test1;

insert into User
select * from MovieLens_Small.User 
where user_id not in ( select user_id from MovieLens_Small_Training1.User );

insert into u2base 
select * from MovieLens_Small.u2base 
where user_id in (select user_id from MovieLens_Small_Test1.User);

insert into item2
select * from MovieLens_Small.item2 
where item_id in (select item_id from MovieLens_Small_Test1.u2base);


select distinct user_id from u2base; -- 20
select distinct item_id from item2;  -- 565
select distinct item_id,user_id, rating from u2base;  -- 1237



/**
. movielens_small: random select 100 user from MovieLens_std, then split 80% training, 20% test

Total tuples: 8754

Total Learning time: 2.872 s
 Building Time(ms) for ALL CT tables:  456 ms.
 CSVPrecomputor TOTAL Time(ms): 42 ms.
 Structure Learning Time(ms): 478 ms.
 Parameter Learning Time(ms): 1896 ms.


CLL -0.43
select sum(loglikelihood)/count(*) from `Gender(User0)_Score`;
-- -0.4256454099150685

AUC PR 1.0

0.5485168184141956	M
0.5639662034566713	M
0.5657579659993108	M
0.5662005638532076	M
0.5680284186478828	M
0.5729072395620642	M
0.6197601917218309	M
0.6257594657504473	M
0.6367356011224649	M
0.6414209155019525	M
0.6491524182030411	M
0.686033896715721	M
0.7002456031972895	M
0.7176253833225102	M
0.7226326671327308	M
0.7398447378711119	M
0.747591188415597	M
0.7482416465362837	M
0.755769034022614	M
0.7774374407156042	M


**/

/*
RDN-Boost only one folder result, average over Gender_F and Gender_M.
PR = 0.590928
CLL = -0.651178

---------------
MLN-Boost only one folder result, average over Gender_F and Gender_M.
PR = 0.5671305
CLL = -1.4858615

*/