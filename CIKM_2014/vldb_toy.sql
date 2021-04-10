-- create a toy eample for vldb paper #Dec 23rd, 2013 @zhensong
Drop database if exists `vldb_toy`;
CREATE DATABASE `vldb_toy`;
use vldb_toy;

CREATE TABLE `Course` (
  `course_id` int(11) NOT NULL,
  `rating` varchar(45) DEFAULT NULL,
  `diff` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`course_id`),
  KEY `course_rating` (`rating`),
  KEY `course_diff` (`diff`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


CREATE TABLE `Professor` (
  `prof_id` varchar(45) NOT NULL DEFAULT '0',
  `popularity` varchar(45) DEFAULT NULL,
  `teachingability` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`prof_id`),
  KEY `prof_popularity` (`popularity`),
  KEY `prof_teachingability` (`teachingability`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


CREATE TABLE `Student` (
  `student_id` varchar(45) NOT NULL DEFAULT '0',
  `intelligence` varchar(45) DEFAULT NULL,
  `ranking` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`student_id`),
  KEY `student_intelligence` (`intelligence`),
  KEY `student_ranking` (`ranking`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


CREATE TABLE `RA` (
	`student_id` varchar(45) NOT NULL DEFAULT '0',  
	`prof_id` varchar(45) NOT NULL DEFAULT '0',
	`salary` varchar(45) DEFAULT NULL,
	`capability` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`prof_id`,`student_id`),
  KEY `FK_u2base_1` (`student_id`),
  KEY `FK_u2base_2` (`prof_id`),
  KEY `RA_capability` (`capability`),
  KEY `RA_salary` (`salary`),
  CONSTRAINT `FK_RA_1` FOREIGN KEY (`student_id`) REFERENCES `Student` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FK_RA_2` FOREIGN KEY (`prof_id`) REFERENCES `Professor` (`prof_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=FIXED;

CREATE TABLE `Registration` (
`student_id` varchar(45) NOT NULL DEFAULT '0',  
`course_id` int(11) NOT NULL DEFAULT '0',
   `grade` varchar(45) NOT NULL,
  `sat` varchar(45) NOT NULL,
  PRIMARY KEY (`course_id`,`student_id`),
  KEY `FK_u2base_1` (`student_id`),
  KEY `FK_u2base_2` (`course_id`),
  KEY `registration_sat` (`sat`),
  KEY `registration_grade` (`grade`),
  CONSTRAINT `FK_registration_1` FOREIGN KEY (`student_id`) REFERENCES `Student` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FK_registration_2` FOREIGN KEY (`course_id`) REFERENCES `Course` (`course_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=FIXED;

INSERT INTO `vldb_toy`.`Course`
(`course_id`,`rating`,`diff`)
VALUES
(101,3,2),
(102,2,1);
INSERT INTO `vldb_toy`.`Professor`
(`prof_id`,`popularity`,`teachingability`)
VALUES
('Oliver',3,1),
('Jim',2,1);
INSERT INTO `vldb_toy`.`Student`
(`student_id`,`intelligence`,`ranking`)
VALUES
('Jack',3,1),
('Kim',2,1),
('Paul',1,2);

INSERT INTO `vldb_toy`.`RA`
(`student_id`,`prof_id`,`salary`,`capability`)
VALUES
('Jack','Oliver','High',3),
('Kim','Oliver','Low',1),
('Paul','Jim','Med',2);

INSERT INTO `vldb_toy`.`Registration`
(`student_id`,`course_id`,`grade`,`sat`)
VALUES
('Jack',101,'A',1),
('Jack',102,'B',2),
('Kim',102,'A',1),
('Paul',101,'B',1);




