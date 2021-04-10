-- --------------------------------------------------------
-- Host:                         cs-oschulte-01.cs.sfu.ca
-- Server version:               5.5.34 - MySQL Community Server (GPL) by Remi
-- Server OS:                    Linux
-- HeidiSQL Version:             8.3.0.4694
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

-- Dumping database structure for unielwin_BN
DROP DATABASE IF EXISTS `unielwin_BN_SIGMOD`;
CREATE DATABASE IF NOT EXISTS `unielwin_BN_SIGMOD` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `unielwin_BN_SIGMOD`;


-- Dumping structure for table unielwin_BN.1NodePars
DROP TABLE IF EXISTS `1NodePars`;
CREATE TABLE IF NOT EXISTS `1NodePars` (
  `ChildNode` varchar(197) NOT NULL DEFAULT '',
  `NumPars` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.1NodePars: ~12 rows (approximately)
DELETE FROM `1NodePars`;
/*!40000 ALTER TABLE `1NodePars` DISABLE KEYS */;
INSERT INTO `1NodePars` (`ChildNode`, `NumPars`) VALUES
	('`a`', 2),
	('`b`', 6),
	('`grade(course0,student0)`', 3.0000000000000004),
	('`popularity(prof0)`', 2),
	('`ranking(student0)`', 3.0000000000000004),
	('`capability(prof0,student0)`', 1),
	('`diff(course0)`', 1),
	('`intelligence(student0)`', 1),
	('`rating(course0)`', 1),
	('`salary(prof0,student0)`', 1),
	('`sat(course0,student0)`', 1),
	('`teachingability(prof0)`', 1);
/*!40000 ALTER TABLE `1NodePars` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.1Nodes
DROP TABLE IF EXISTS `1Nodes`;
CREATE TABLE IF NOT EXISTS `1Nodes` (
  `1nid` varchar(133) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `COLUMN_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `main` int(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.1Nodes: ~6 rows (approximately)
DELETE FROM `1Nodes`;
/*!40000 ALTER TABLE `1Nodes` DISABLE KEYS */;
INSERT INTO `1Nodes` (`1nid`, `COLUMN_NAME`, `pvid`, `main`) VALUES
	('`diff(course0)`', 'diff', 'course0', 1),
	('`intelligence(student0)`', 'intelligence', 'student0', 1),
	('`popularity(prof0)`', 'popularity', 'prof0', 1),
	('`ranking(student0)`', 'ranking', 'student0', 1),
	('`rating(course0)`', 'rating', 'course0', 1),
	('`teachingability(prof0)`', 'teachingability', 'prof0', 1);
/*!40000 ALTER TABLE `1Nodes` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.1Nodes_From_List
DROP TABLE IF EXISTS `1Nodes_From_List`;
CREATE TABLE IF NOT EXISTS `1Nodes_From_List` (
  `1nid` varchar(133) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `Entries` varchar(133) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.1Nodes_From_List: ~6 rows (approximately)
DELETE FROM `1Nodes_From_List`;
/*!40000 ALTER TABLE `1Nodes_From_List` DISABLE KEYS */;
INSERT INTO `1Nodes_From_List` (`1nid`, `Entries`) VALUES
	('`diff(course0)`', 'course AS course0'),
	('`intelligence(student0)`', 'student AS student0'),
	('`popularity(prof0)`', 'prof AS prof0'),
	('`ranking(student0)`', 'student AS student0'),
	('`rating(course0)`', 'course AS course0'),
	('`teachingability(prof0)`', 'prof AS prof0');
/*!40000 ALTER TABLE `1Nodes_From_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.1Nodes_inFamily
DROP TABLE IF EXISTS `1Nodes_inFamily`;
CREATE TABLE IF NOT EXISTS `1Nodes_inFamily` (
  `ChildNode` varchar(197) NOT NULL,
  `1node` varchar(197) NOT NULL,
  `NumAtts` bigint(21) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.1Nodes_inFamily: ~6 rows (approximately)
DELETE FROM `1Nodes_inFamily`;
/*!40000 ALTER TABLE `1Nodes_inFamily` DISABLE KEYS */;
INSERT INTO `1Nodes_inFamily` (`ChildNode`, `1node`, `NumAtts`) VALUES
	('`b`', '`intelligence(student0)`', 3),
	('`grade(course0,student0)`', '`intelligence(student0)`', 3),
	('`ranking(student0)`', '`intelligence(student0)`', 3),
	('`b`', '`rating(course0)`', 2),
	('`a`', '`teachingability(prof0)`', 2),
	('`popularity(prof0)`', '`teachingability(prof0)`', 2);
/*!40000 ALTER TABLE `1Nodes_inFamily` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.1Nodes_Select_List
DROP TABLE IF EXISTS `1Nodes_Select_List`;
CREATE TABLE IF NOT EXISTS `1Nodes_Select_List` (
  `1nid` varchar(133) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `Entries` varchar(267) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.1Nodes_Select_List: ~6 rows (approximately)
DELETE FROM `1Nodes_Select_List`;
/*!40000 ALTER TABLE `1Nodes_Select_List` DISABLE KEYS */;
INSERT INTO `1Nodes_Select_List` (`1nid`, `Entries`) VALUES
	('`diff(course0)`', 'course0.diff AS `diff(course0)`'),
	('`intelligence(student0)`', 'student0.intelligence AS `intelligence(student0)`'),
	('`popularity(prof0)`', 'prof0.popularity AS `popularity(prof0)`'),
	('`ranking(student0)`', 'student0.ranking AS `ranking(student0)`'),
	('`rating(course0)`', 'course0.rating AS `rating(course0)`'),
	('`teachingability(prof0)`', 'prof0.teachingability AS `teachingability(prof0)`');
/*!40000 ALTER TABLE `1Nodes_Select_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.2Nodes
DROP TABLE IF EXISTS `2Nodes`;
CREATE TABLE IF NOT EXISTS `2Nodes` (
  `2nid` varchar(199) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `COLUMN_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `pvid1` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `pvid2` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `main` int(11) NOT NULL DEFAULT '0',
  KEY `index` (`pvid1`,`pvid2`,`TABLE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.2Nodes: ~4 rows (approximately)
DELETE FROM `2Nodes`;
/*!40000 ALTER TABLE `2Nodes` DISABLE KEYS */;
INSERT INTO `2Nodes` (`2nid`, `COLUMN_NAME`, `pvid1`, `pvid2`, `TABLE_NAME`, `main`) VALUES
	('`capability(prof0,student0)`', 'capability', 'prof0', 'student0', 'RA', 1),
	('`grade(course0,student0)`', 'grade', 'course0', 'student0', 'registration', 1),
	('`salary(prof0,student0)`', 'salary', 'prof0', 'student0', 'RA', 1),
	('`sat(course0,student0)`', 'sat', 'course0', 'student0', 'registration', 1);
/*!40000 ALTER TABLE `2Nodes` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.2Nodes_From_List
DROP TABLE IF EXISTS `2Nodes_From_List`;
CREATE TABLE IF NOT EXISTS `2Nodes_From_List` (
  `2nid` varchar(199) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `Entries` varchar(78) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.2Nodes_From_List: ~4 rows (approximately)
DELETE FROM `2Nodes_From_List`;
/*!40000 ALTER TABLE `2Nodes_From_List` DISABLE KEYS */;
INSERT INTO `2Nodes_From_List` (`2nid`, `Entries`) VALUES
	('`capability(prof0,student0)`', 'RA AS `a`'),
	('`grade(course0,student0)`', 'registration AS `b`'),
	('`salary(prof0,student0)`', 'RA AS `a`'),
	('`sat(course0,student0)`', 'registration AS `b`');
/*!40000 ALTER TABLE `2Nodes_From_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.2Nodes_inFamily
DROP TABLE IF EXISTS `2Nodes_inFamily`;
CREATE TABLE IF NOT EXISTS `2Nodes_inFamily` (
  `ChildNode` varchar(197) NOT NULL,
  `2node` varchar(197) NOT NULL,
  `NumAtts` bigint(21) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.2Nodes_inFamily: ~3 rows (approximately)
DELETE FROM `2Nodes_inFamily`;
/*!40000 ALTER TABLE `2Nodes_inFamily` DISABLE KEYS */;
INSERT INTO `2Nodes_inFamily` (`ChildNode`, `2node`, `NumAtts`) VALUES
	('`salary(prof0,student0)`', '`capability(prof0,student0)`', 5),
	('`diff(course0)`', '`grade(course0,student0)`', 4),
	('`sat(course0,student0)`', '`grade(course0,student0)`', 4);
/*!40000 ALTER TABLE `2Nodes_inFamily` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.2Nodes_Select_List
DROP TABLE IF EXISTS `2Nodes_Select_List`;
CREATE TABLE IF NOT EXISTS `2Nodes_Select_List` (
  `2nid` varchar(199) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `Entries` varchar(278) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.2Nodes_Select_List: ~4 rows (approximately)
DELETE FROM `2Nodes_Select_List`;
/*!40000 ALTER TABLE `2Nodes_Select_List` DISABLE KEYS */;
INSERT INTO `2Nodes_Select_List` (`2nid`, `Entries`) VALUES
	('`capability(prof0,student0)`', '`a`.capability AS `capability(prof0,student0)`'),
	('`grade(course0,student0)`', '`b`.grade AS `grade(course0,student0)`'),
	('`salary(prof0,student0)`', '`a`.salary AS `salary(prof0,student0)`'),
	('`sat(course0,student0)`', '`b`.sat AS `sat(course0,student0)`');
/*!40000 ALTER TABLE `2Nodes_Select_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_PVariables_From_List
DROP TABLE IF EXISTS `ADT_PVariables_From_List`;
CREATE TABLE IF NOT EXISTS `ADT_PVariables_From_List` (
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `Entries` varchar(142) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_PVariables_From_List: ~3 rows (approximately)
DELETE FROM `ADT_PVariables_From_List`;
/*!40000 ALTER TABLE `ADT_PVariables_From_List` DISABLE KEYS */;
INSERT INTO `ADT_PVariables_From_List` (`pvid`, `Entries`) VALUES
	('course0', 'unielwin.course AS course0'),
	('prof0', 'unielwin.prof AS prof0'),
	('student0', 'unielwin.student AS student0');
/*!40000 ALTER TABLE `ADT_PVariables_From_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_PVariables_GroupBy_List
DROP TABLE IF EXISTS `ADT_PVariables_GroupBy_List`;
CREATE TABLE IF NOT EXISTS `ADT_PVariables_GroupBy_List` (
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `Entries` varchar(133) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_PVariables_GroupBy_List: ~6 rows (approximately)
DELETE FROM `ADT_PVariables_GroupBy_List`;
/*!40000 ALTER TABLE `ADT_PVariables_GroupBy_List` DISABLE KEYS */;
INSERT INTO `ADT_PVariables_GroupBy_List` (`pvid`, `Entries`) VALUES
	('course0', '`diff(course0)`'),
	('student0', '`intelligence(student0)`'),
	('prof0', '`popularity(prof0)`'),
	('student0', '`ranking(student0)`'),
	('course0', '`rating(course0)`'),
	('prof0', '`teachingability(prof0)`');
/*!40000 ALTER TABLE `ADT_PVariables_GroupBy_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_PVariables_Select_List
DROP TABLE IF EXISTS `ADT_PVariables_Select_List`;
CREATE TABLE IF NOT EXISTS `ADT_PVariables_Select_List` (
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `Entries` varchar(267) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_PVariables_Select_List: ~9 rows (approximately)
DELETE FROM `ADT_PVariables_Select_List`;
/*!40000 ALTER TABLE `ADT_PVariables_Select_List` DISABLE KEYS */;
INSERT INTO `ADT_PVariables_Select_List` (`pvid`, `Entries`) VALUES
	('course0', 'count(*) as "MULT"'),
	('prof0', 'count(*) as "MULT"'),
	('student0', 'count(*) as "MULT"'),
	('course0', 'course0.diff AS `diff(course0)`'),
	('student0', 'student0.intelligence AS `intelligence(student0)`'),
	('prof0', 'prof0.popularity AS `popularity(prof0)`'),
	('student0', 'student0.ranking AS `ranking(student0)`'),
	('course0', 'course0.rating AS `rating(course0)`'),
	('prof0', 'prof0.teachingability AS `teachingability(prof0)`');
/*!40000 ALTER TABLE `ADT_PVariables_Select_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_RChain_Star_From_List
DROP TABLE IF EXISTS `ADT_RChain_Star_From_List`;
CREATE TABLE IF NOT EXISTS `ADT_RChain_Star_From_List` (
  `rchain` varchar(20) NOT NULL DEFAULT '',
  `rnid` varchar(20) DEFAULT NULL,
  `Entries` varchar(74) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_RChain_Star_From_List: ~4 rows (approximately)
DELETE FROM `ADT_RChain_Star_From_List`;
/*!40000 ALTER TABLE `ADT_RChain_Star_From_List` DISABLE KEYS */;
INSERT INTO `ADT_RChain_Star_From_List` (`rchain`, `rnid`, `Entries`) VALUES
	('`a,b`', '`b`', '`a_CT`'),
	('`a,b`', '`a`', '`b_CT`'),
	('`a,b`', '`a`', '`prof0_counts`'),
	('`a,b`', '`b`', '`course0_counts`');
/*!40000 ALTER TABLE `ADT_RChain_Star_From_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_RChain_Star_Select_List
DROP TABLE IF EXISTS `ADT_RChain_Star_Select_List`;
CREATE TABLE IF NOT EXISTS `ADT_RChain_Star_Select_List` (
  `rchain` varchar(20) DEFAULT NULL,
  `rnid` varchar(20) DEFAULT NULL,
  `Entries` varchar(199) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_RChain_Star_Select_List: ~26 rows (approximately)
DELETE FROM `ADT_RChain_Star_Select_List`;
/*!40000 ALTER TABLE `ADT_RChain_Star_Select_List` DISABLE KEYS */;
INSERT INTO `ADT_RChain_Star_Select_List` (`rchain`, `rnid`, `Entries`) VALUES
	('`a,b`', '`a`', '`diff(course0)`'),
	('`a,b`', '`b`', '`popularity(prof0)`'),
	('`a,b`', '`a`', '`rating(course0)`'),
	('`a,b`', '`b`', '`teachingability(prof0)`'),
	('`a,b`', '`b`', '`intelligence(student0)`'),
	('`a,b`', '`a`', '`intelligence(student0)`'),
	('`a,b`', '`b`', '`ranking(student0)`'),
	('`a,b`', '`a`', '`ranking(student0)`'),
	('`a,b`', '`b`', '`capability(prof0,student0)`'),
	('`a,b`', '`a`', '`grade(course0,student0)`'),
	('`a,b`', '`b`', '`salary(prof0,student0)`'),
	('`a,b`', '`a`', '`sat(course0,student0)`'),
	('`a,b`', '`b`', '`a`'),
	('`a,b`', '`a`', '`b`'),
	('`a,b`', '`b`', '`diff(course0)`'),
	('`a,b`', '`a`', '`popularity(prof0)`'),
	('`a,b`', '`b`', '`rating(course0)`'),
	('`a,b`', '`a`', '`teachingability(prof0)`'),
	('`b`', '`b`', '`diff(course0)`'),
	('`a`', '`a`', '`intelligence(student0)`'),
	('`b`', '`b`', '`intelligence(student0)`'),
	('`a`', '`a`', '`popularity(prof0)`'),
	('`a`', '`a`', '`ranking(student0)`'),
	('`b`', '`b`', '`ranking(student0)`'),
	('`b`', '`b`', '`rating(course0)`'),
	('`a`', '`a`', '`teachingability(prof0)`');
/*!40000 ALTER TABLE `ADT_RChain_Star_Select_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_RChain_Star_Where_List
DROP TABLE IF EXISTS `ADT_RChain_Star_Where_List`;
CREATE TABLE IF NOT EXISTS `ADT_RChain_Star_Where_List` (
  `rchain` varchar(20) NOT NULL DEFAULT '',
  `rnid` varchar(20) DEFAULT NULL,
  `Entries` varchar(26) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_RChain_Star_Where_List: ~1 rows (approximately)
DELETE FROM `ADT_RChain_Star_Where_List`;
/*!40000 ALTER TABLE `ADT_RChain_Star_Where_List` DISABLE KEYS */;
INSERT INTO `ADT_RChain_Star_Where_List` (`rchain`, `rnid`, `Entries`) VALUES
	('`a,b`', '`a`', '`b` = "T"');
/*!40000 ALTER TABLE `ADT_RChain_Star_Where_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_RNodes_1Nodes_FROM_List
DROP TABLE IF EXISTS `ADT_RNodes_1Nodes_FROM_List`;
CREATE TABLE IF NOT EXISTS `ADT_RNodes_1Nodes_FROM_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(19) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_RNodes_1Nodes_FROM_List: ~2 rows (approximately)
DELETE FROM `ADT_RNodes_1Nodes_FROM_List`;
/*!40000 ALTER TABLE `ADT_RNodes_1Nodes_FROM_List` DISABLE KEYS */;
INSERT INTO `ADT_RNodes_1Nodes_FROM_List` (`rnid`, `Entries`) VALUES
	('`a`', '`a_counts`'),
	('`b`', '`b_counts`');
/*!40000 ALTER TABLE `ADT_RNodes_1Nodes_FROM_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_RNodes_1Nodes_GroupBY_List
DROP TABLE IF EXISTS `ADT_RNodes_1Nodes_GroupBY_List`;
CREATE TABLE IF NOT EXISTS `ADT_RNodes_1Nodes_GroupBY_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(133) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_RNodes_1Nodes_GroupBY_List: ~8 rows (approximately)
DELETE FROM `ADT_RNodes_1Nodes_GroupBY_List`;
/*!40000 ALTER TABLE `ADT_RNodes_1Nodes_GroupBY_List` DISABLE KEYS */;
INSERT INTO `ADT_RNodes_1Nodes_GroupBY_List` (`rnid`, `Entries`) VALUES
	('`b`', '`diff(course0)`'),
	('`a`', '`popularity(prof0)`'),
	('`b`', '`rating(course0)`'),
	('`a`', '`teachingability(prof0)`'),
	('`a`', '`intelligence(student0)`'),
	('`b`', '`intelligence(student0)`'),
	('`a`', '`ranking(student0)`'),
	('`b`', '`ranking(student0)`');
/*!40000 ALTER TABLE `ADT_RNodes_1Nodes_GroupBY_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_RNodes_1Nodes_Select_List
DROP TABLE IF EXISTS `ADT_RNodes_1Nodes_Select_List`;
CREATE TABLE IF NOT EXISTS `ADT_RNodes_1Nodes_Select_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(133) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_RNodes_1Nodes_Select_List: ~10 rows (approximately)
DELETE FROM `ADT_RNodes_1Nodes_Select_List`;
/*!40000 ALTER TABLE `ADT_RNodes_1Nodes_Select_List` DISABLE KEYS */;
INSERT INTO `ADT_RNodes_1Nodes_Select_List` (`rnid`, `Entries`) VALUES
	('`a`', 'sum(`a_counts`.`MULT`) as "MULT"'),
	('`b`', 'sum(`b_counts`.`MULT`) as "MULT"'),
	('`b`', '`diff(course0)`'),
	('`a`', '`popularity(prof0)`'),
	('`b`', '`rating(course0)`'),
	('`a`', '`teachingability(prof0)`'),
	('`a`', '`intelligence(student0)`'),
	('`b`', '`intelligence(student0)`'),
	('`a`', '`ranking(student0)`'),
	('`b`', '`ranking(student0)`');
/*!40000 ALTER TABLE `ADT_RNodes_1Nodes_Select_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_RNodes_False_FROM_List
DROP TABLE IF EXISTS `ADT_RNodes_False_FROM_List`;
CREATE TABLE IF NOT EXISTS `ADT_RNodes_False_FROM_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(17) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_RNodes_False_FROM_List: ~4 rows (approximately)
DELETE FROM `ADT_RNodes_False_FROM_List`;
/*!40000 ALTER TABLE `ADT_RNodes_False_FROM_List` DISABLE KEYS */;
INSERT INTO `ADT_RNodes_False_FROM_List` (`rnid`, `Entries`) VALUES
	('`a`', '`a_star`'),
	('`b`', '`b_star`'),
	('`a`', '`a_flat`'),
	('`b`', '`b_flat`');
/*!40000 ALTER TABLE `ADT_RNodes_False_FROM_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_RNodes_False_Select_List
DROP TABLE IF EXISTS `ADT_RNodes_False_Select_List`;
CREATE TABLE IF NOT EXISTS `ADT_RNodes_False_Select_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(151) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_RNodes_False_Select_List: ~10 rows (approximately)
DELETE FROM `ADT_RNodes_False_Select_List`;
/*!40000 ALTER TABLE `ADT_RNodes_False_Select_List` DISABLE KEYS */;
INSERT INTO `ADT_RNodes_False_Select_List` (`rnid`, `Entries`) VALUES
	('`a`', '(`a_star`.MULT-`a_flat`.MULT) AS "MULT"'),
	('`b`', '(`b_star`.MULT-`b_flat`.MULT) AS "MULT"'),
	('`b`', '`b_star`.`diff(course0)`'),
	('`a`', '`a_star`.`popularity(prof0)`'),
	('`b`', '`b_star`.`rating(course0)`'),
	('`a`', '`a_star`.`teachingability(prof0)`'),
	('`a`', '`a_star`.`intelligence(student0)`'),
	('`b`', '`b_star`.`intelligence(student0)`'),
	('`a`', '`a_star`.`ranking(student0)`'),
	('`b`', '`b_star`.`ranking(student0)`');
/*!40000 ALTER TABLE `ADT_RNodes_False_Select_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_RNodes_False_WHERE_List
DROP TABLE IF EXISTS `ADT_RNodes_False_WHERE_List`;
CREATE TABLE IF NOT EXISTS `ADT_RNodes_False_WHERE_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(303) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_RNodes_False_WHERE_List: ~8 rows (approximately)
DELETE FROM `ADT_RNodes_False_WHERE_List`;
/*!40000 ALTER TABLE `ADT_RNodes_False_WHERE_List` DISABLE KEYS */;
INSERT INTO `ADT_RNodes_False_WHERE_List` (`rnid`, `Entries`) VALUES
	('`b`', '`b_star`.`diff(course0)`=`b_flat`.`diff(course0)`'),
	('`a`', '`a_star`.`popularity(prof0)`=`a_flat`.`popularity(prof0)`'),
	('`b`', '`b_star`.`rating(course0)`=`b_flat`.`rating(course0)`'),
	('`a`', '`a_star`.`teachingability(prof0)`=`a_flat`.`teachingability(prof0)`'),
	('`a`', '`a_star`.`intelligence(student0)`=`a_flat`.`intelligence(student0)`'),
	('`b`', '`b_star`.`intelligence(student0)`=`b_flat`.`intelligence(student0)`'),
	('`a`', '`a_star`.`ranking(student0)`=`a_flat`.`ranking(student0)`'),
	('`b`', '`b_star`.`ranking(student0)`=`b_flat`.`ranking(student0)`');
/*!40000 ALTER TABLE `ADT_RNodes_False_WHERE_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_RNodes_Star_From_List
DROP TABLE IF EXISTS `ADT_RNodes_Star_From_List`;
CREATE TABLE IF NOT EXISTS `ADT_RNodes_Star_From_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(74) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_RNodes_Star_From_List: ~4 rows (approximately)
DELETE FROM `ADT_RNodes_Star_From_List`;
/*!40000 ALTER TABLE `ADT_RNodes_Star_From_List` DISABLE KEYS */;
INSERT INTO `ADT_RNodes_Star_From_List` (`rnid`, `Entries`) VALUES
	('`a`', '`prof0_counts`'),
	('`b`', '`course0_counts`'),
	('`a`', '`student0_counts`'),
	('`b`', '`student0_counts`');
/*!40000 ALTER TABLE `ADT_RNodes_Star_From_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ADT_RNodes_Star_Select_List
DROP TABLE IF EXISTS `ADT_RNodes_Star_Select_List`;
CREATE TABLE IF NOT EXISTS `ADT_RNodes_Star_Select_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(133) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ADT_RNodes_Star_Select_List: ~8 rows (approximately)
DELETE FROM `ADT_RNodes_Star_Select_List`;
/*!40000 ALTER TABLE `ADT_RNodes_Star_Select_List` DISABLE KEYS */;
INSERT INTO `ADT_RNodes_Star_Select_List` (`rnid`, `Entries`) VALUES
	('`b`', '`diff(course0)`'),
	('`a`', '`popularity(prof0)`'),
	('`b`', '`rating(course0)`'),
	('`a`', '`teachingability(prof0)`'),
	('`a`', '`intelligence(student0)`'),
	('`b`', '`intelligence(student0)`'),
	('`a`', '`ranking(student0)`'),
	('`b`', '`ranking(student0)`');
/*!40000 ALTER TABLE `ADT_RNodes_Star_Select_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.AttributeColumns
DROP TABLE IF EXISTS `AttributeColumns`;
CREATE TABLE IF NOT EXISTS `AttributeColumns` (
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `COLUMN_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.AttributeColumns: ~10 rows (approximately)
DELETE FROM `AttributeColumns`;
/*!40000 ALTER TABLE `AttributeColumns` DISABLE KEYS */;
INSERT INTO `AttributeColumns` (`TABLE_NAME`, `COLUMN_NAME`) VALUES
	('course', 'diff'),
	('course', 'rating'),
	('prof', 'popularity'),
	('prof', 'teachingability'),
	('RA', 'capability'),
	('RA', 'salary'),
	('registration', 'grade'),
	('registration', 'sat'),
	('student', 'intelligence'),
	('student', 'ranking');
/*!40000 ALTER TABLE `AttributeColumns` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Attribute_Value
DROP TABLE IF EXISTS `Attribute_Value`;
CREATE TABLE IF NOT EXISTS `Attribute_Value` (
  `COLUMN_NAME` varchar(30) DEFAULT NULL,
  `VALUE` varchar(30) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Attribute_Value: ~35 rows (approximately)
DELETE FROM `Attribute_Value`;
/*!40000 ALTER TABLE `Attribute_Value` DISABLE KEYS */;
INSERT INTO `Attribute_Value` (`COLUMN_NAME`, `VALUE`) VALUES
	('diff', '1'),
	('diff', '2'),
	('rating', '1'),
	('rating', '2'),
	('popularity', '1'),
	('popularity', '2'),
	('teachingability', '2'),
	('teachingability', '3'),
	('capability', '1'),
	('capability', '2'),
	('capability', '3'),
	('capability', '4'),
	('capability', '5'),
	('salary', 'high'),
	('salary', 'low'),
	('salary', 'med'),
	('grade', '1'),
	('grade', '2'),
	('grade', '3'),
	('grade', '4'),
	('sat', '1'),
	('sat', '2'),
	('sat', '3'),
	('intelligence', '1'),
	('intelligence', '2'),
	('intelligence', '3'),
	('ranking', '1'),
	('ranking', '2'),
	('ranking', '3'),
	('ranking', '4'),
	('ranking', '5'),
	('`a`', 'T'),
	('`a`', 'F'),
	('`b`', 'T'),
	('`b`', 'F');
/*!40000 ALTER TABLE `Attribute_Value` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.a_CP
DROP TABLE IF EXISTS `a_CP`;
CREATE TABLE IF NOT EXISTS `a_CP` (
  `RA(prof0,student0)` varchar(5) DEFAULT NULL,
  `teachingability(prof0)` varchar(45) DEFAULT NULL,
  `ParentSum` bigint(20) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL,
  KEY `a_CP` (`RA(prof0,student0)`,`teachingability(prof0)`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.a_CP: ~4 rows (approximately)
DELETE FROM `a_CP`;
/*!40000 ALTER TABLE `a_CP` DISABLE KEYS */;
INSERT INTO `a_CP` (`RA(prof0,student0)`, `teachingability(prof0)`, `ParentSum`, `local_mult`, `CP`, `likelihood`, `prior`) VALUES
	('F', '2', 1140, 104, 0.912281, -9.55, 0.890351),
	('F', '3', 1140, 99, 0.868421, -13.97, 0.890351),
	('T', '2', 1140, 10, 0.087719, -24.34, 0.109649),
	('T', '3', 1140, 15, 0.131579, -30.42, 0.109649);
/*!40000 ALTER TABLE `a_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.b_CP
DROP TABLE IF EXISTS `b_CP`;
CREATE TABLE IF NOT EXISTS `b_CP` (
  `registration(course0,student0)` varchar(5) DEFAULT NULL,
  `intelligence(student0)` varchar(45) DEFAULT NULL,
  `rating(course0)` varchar(45) DEFAULT NULL,
  `ParentSum` bigint(20) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL,
  KEY `b_CP` (`registration(course0,student0)`,`intelligence(student0)`,`rating(course0)`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.b_CP: ~12 rows (approximately)
DELETE FROM `b_CP`;
/*!40000 ALTER TABLE `b_CP` DISABLE KEYS */;
INSERT INTO `b_CP` (`registration(course0,student0)`, `intelligence(student0)`, `rating(course0)`, `ParentSum`, `local_mult`, `CP`, `likelihood`, `prior`) VALUES
	('F', '1', '1', 252, 31, 0.738095, -9.41, 0.757895),
	('F', '1', '2', 588, 83, 0.846939, -13.79, 0.757895),
	('F', '2', '1', 234, 27, 0.692308, -9.93, 0.757895),
	('F', '2', '2', 546, 73, 0.802198, -16.09, 0.757895),
	('F', '3', '1', 198, 25, 0.757576, -6.94, 0.757895),
	('F', '3', '2', 462, 49, 0.636364, -22.15, 0.757895),
	('T', '1', '1', 252, 11, 0.261905, -14.74, 0.242105),
	('T', '1', '2', 588, 15, 0.153061, -28.15, 0.242105),
	('T', '2', '1', 234, 12, 0.307692, -14.14, 0.242105),
	('T', '2', '2', 546, 18, 0.197802, -29.17, 0.242105),
	('T', '3', '1', 198, 8, 0.242424, -11.34, 0.242105),
	('T', '3', '2', 462, 28, 0.363636, -28.32, 0.242105);
/*!40000 ALTER TABLE `b_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.capability(prof0,student0)_CP
DROP TABLE IF EXISTS `capability(prof0,student0)_CP`;
CREATE TABLE IF NOT EXISTS `capability(prof0,student0)_CP` (
  `capability(prof0,student0)` varchar(45) DEFAULT NULL,
  `RA(prof0,student0)` varchar(5) DEFAULT NULL,
  `ParentSum` bigint(20) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL,
  KEY `capability(prof0,student0)_CP` (`capability(prof0,student0)`,`RA(prof0,student0)`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.capability(prof0,student0)_CP: ~6 rows (approximately)
DELETE FROM `capability(prof0,student0)_CP`;
/*!40000 ALTER TABLE `capability(prof0,student0)_CP` DISABLE KEYS */;
INSERT INTO `capability(prof0,student0)_CP` (`capability(prof0,student0)`, `RA(prof0,student0)`, `ParentSum`, `local_mult`, `CP`, `likelihood`, `prior`) VALUES
	('1', 'T', 250, 5, 0.200000, -8.05, 0.021930),
	('2', 'T', 250, 4, 0.160000, -7.33, 0.017544),
	('3', 'T', 250, 7, 0.280000, -8.91, 0.030702),
	('4', 'T', 250, 5, 0.200000, -8.05, 0.021930),
	('5', 'T', 250, 4, 0.160000, -7.33, 0.017544),
	('N/A', 'F', 2030, 203, 1.000000, 0.00, 0.890351);
/*!40000 ALTER TABLE `capability(prof0,student0)_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ChildPars
DROP TABLE IF EXISTS `ChildPars`;
CREATE TABLE IF NOT EXISTS `ChildPars` (
  `NumPars` bigint(22) NOT NULL DEFAULT '0',
  `ChildNode` varchar(199) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ChildPars: ~12 rows (approximately)
DELETE FROM `ChildPars`;
/*!40000 ALTER TABLE `ChildPars` DISABLE KEYS */;
INSERT INTO `ChildPars` (`NumPars`, `ChildNode`) VALUES
	(4, '`capability(prof0,student0)`'),
	(1, '`diff(course0)`'),
	(3, '`grade(course0,student0)`'),
	(2, '`intelligence(student0)`'),
	(1, '`popularity(prof0)`'),
	(4, '`ranking(student0)`'),
	(1, '`rating(course0)`'),
	(2, '`salary(prof0,student0)`'),
	(2, '`sat(course0,student0)`'),
	(1, '`teachingability(prof0)`'),
	(1, '`a`'),
	(1, '`b`');
/*!40000 ALTER TABLE `ChildPars` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ContextEdges
DROP TABLE IF EXISTS `ContextEdges`;
CREATE TABLE IF NOT EXISTS `ContextEdges` (
  `Rchain` varchar(255) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(20) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ContextEdges: ~0 rows (approximately)
DELETE FROM `ContextEdges`;
/*!40000 ALTER TABLE `ContextEdges` DISABLE KEYS */;
/*!40000 ALTER TABLE `ContextEdges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.diff(course0)_CP
DROP TABLE IF EXISTS `diff(course0)_CP`;
CREATE TABLE IF NOT EXISTS `diff(course0)_CP` (
  `diff(course0)` varchar(45) DEFAULT NULL,
  `grade(course0,student0)` varchar(45) DEFAULT NULL,
  `ParentSum` bigint(20) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL,
  KEY `diff(course0)_CP` (`diff(course0)`,`grade(course0,student0)`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.diff(course0)_CP: ~10 rows (approximately)
DELETE FROM `diff(course0)_CP`;
/*!40000 ALTER TABLE `diff(course0)_CP` DISABLE KEYS */;
INSERT INTO `diff(course0)_CP` (`diff(course0)`, `grade(course0,student0)`, `ParentSum`, `local_mult`, `CP`, `likelihood`, `prior`) VALUES
	('1', '1', 192, 26, 0.812500, -5.40, 0.500000),
	('1', '2', 174, 8, 0.275862, -10.30, 0.500000),
	('1', '3', 108, 7, 0.388889, -6.61, 0.500000),
	('1', '4', 78, 6, 0.461538, -4.64, 0.500000),
	('1', 'N/A', 1728, 143, 0.496528, -100.12, 0.500000),
	('2', '1', 192, 6, 0.187500, -10.04, 0.500000),
	('2', '2', 174, 21, 0.724138, -6.78, 0.500000),
	('2', '3', 108, 11, 0.611111, -5.42, 0.500000),
	('2', '4', 78, 7, 0.538462, -4.33, 0.500000),
	('2', 'N/A', 1728, 145, 0.503472, -99.50, 0.500000);
/*!40000 ALTER TABLE `diff(course0)_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.EntityTables
DROP TABLE IF EXISTS `EntityTables`;
CREATE TABLE IF NOT EXISTS `EntityTables` (
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `COLUMN_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.EntityTables: ~3 rows (approximately)
DELETE FROM `EntityTables`;
/*!40000 ALTER TABLE `EntityTables` DISABLE KEYS */;
INSERT INTO `EntityTables` (`TABLE_NAME`, `COLUMN_NAME`) VALUES
	('course', 'course_id'),
	('prof', 'prof_id'),
	('student', 'student_id');
/*!40000 ALTER TABLE `EntityTables` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Entity_BayesNets
DROP TABLE IF EXISTS `Entity_BayesNets`;
CREATE TABLE IF NOT EXISTS `Entity_BayesNets` (
  `pvid` varchar(65) NOT NULL,
  `child` varchar(131) NOT NULL,
  `parent` varchar(131) NOT NULL,
  PRIMARY KEY (`pvid`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Entity_BayesNets: ~6 rows (approximately)
DELETE FROM `Entity_BayesNets`;
/*!40000 ALTER TABLE `Entity_BayesNets` DISABLE KEYS */;
INSERT INTO `Entity_BayesNets` (`pvid`, `child`, `parent`) VALUES
	('course0', '`diff(course0)`', ''),
	('course0', '`rating(course0)`', ''),
	('prof0', '`popularity(prof0)`', '`teachingability(prof0)`'),
	('prof0', '`teachingability(prof0)`', ''),
	('student0', '`intelligence(student0)`', ''),
	('student0', '`ranking(student0)`', '`intelligence(student0)`');
/*!40000 ALTER TABLE `Entity_BayesNets` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Entity_BN_Nodes
DROP TABLE IF EXISTS `Entity_BN_Nodes`;
CREATE TABLE IF NOT EXISTS `Entity_BN_Nodes` (
  `pvid` varchar(65) NOT NULL,
  `node` varchar(131) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Entity_BN_Nodes: ~6 rows (approximately)
DELETE FROM `Entity_BN_Nodes`;
/*!40000 ALTER TABLE `Entity_BN_Nodes` DISABLE KEYS */;
INSERT INTO `Entity_BN_Nodes` (`pvid`, `node`) VALUES
	('course0', '`diff(course0)`'),
	('course0', '`rating(course0)`'),
	('prof0', '`popularity(prof0)`'),
	('prof0', '`teachingability(prof0)`'),
	('student0', '`intelligence(student0)`'),
	('student0', '`ranking(student0)`');
/*!40000 ALTER TABLE `Entity_BN_Nodes` ENABLE KEYS */;


-- Dumping structure for view unielwin_BN.Entity_BN_nodes
DROP VIEW IF EXISTS `Entity_BN_nodes`;
-- Creating temporary table to overcome VIEW dependency errors
CREATE TABLE `Entity_BN_nodes` (
	`pvid` VARCHAR(65) NOT NULL COLLATE 'latin1_swedish_ci',
	`node` VARCHAR(131) NOT NULL COLLATE 'latin1_swedish_ci'
) ENGINE=MyISAM;


-- Dumping structure for table unielwin_BN.Entity_Complement_Edges
DROP TABLE IF EXISTS `Entity_Complement_Edges`;
CREATE TABLE IF NOT EXISTS `Entity_Complement_Edges` (
  `pvid` varchar(65) NOT NULL,
  `child` varchar(131) NOT NULL,
  `parent` varchar(131) NOT NULL,
  PRIMARY KEY (`pvid`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Entity_Complement_Edges: ~10 rows (approximately)
DELETE FROM `Entity_Complement_Edges`;
/*!40000 ALTER TABLE `Entity_Complement_Edges` DISABLE KEYS */;
INSERT INTO `Entity_Complement_Edges` (`pvid`, `child`, `parent`) VALUES
	('course0', '`diff(course0)`', '`diff(course0)`'),
	('course0', '`diff(course0)`', '`rating(course0)`'),
	('course0', '`rating(course0)`', '`diff(course0)`'),
	('course0', '`rating(course0)`', '`rating(course0)`'),
	('prof0', '`popularity(prof0)`', '`popularity(prof0)`'),
	('prof0', '`teachingability(prof0)`', '`popularity(prof0)`'),
	('prof0', '`teachingability(prof0)`', '`teachingability(prof0)`'),
	('student0', '`intelligence(student0)`', '`intelligence(student0)`'),
	('student0', '`intelligence(student0)`', '`ranking(student0)`'),
	('student0', '`ranking(student0)`', '`ranking(student0)`');
/*!40000 ALTER TABLE `Entity_Complement_Edges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Final_Path_BayesNets
DROP TABLE IF EXISTS `Final_Path_BayesNets`;
CREATE TABLE IF NOT EXISTS `Final_Path_BayesNets` (
  `Rchain` varchar(256) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(197) NOT NULL,
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Final_Path_BayesNets: ~16 rows (approximately)
DELETE FROM `Final_Path_BayesNets`;
/*!40000 ALTER TABLE `Final_Path_BayesNets` DISABLE KEYS */;
INSERT INTO `Final_Path_BayesNets` (`Rchain`, `child`, `parent`) VALUES
	('`RA(prof0,student0),registration(course0,student0)`', '`capability(prof0,student0)`', '`RA(prof0,student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`diff(course0)`', '`grade(course0,student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`grade(course0,student0)`', '`intelligence(student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`grade(course0,student0)`', '`registration(course0,student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`intelligence(student0)`', '`RA(prof0,student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`popularity(prof0)`', '`RA(prof0,student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`popularity(prof0)`', '`teachingability(prof0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`RA(prof0,student0)`', '`teachingability(prof0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`ranking(student0)`', '`intelligence(student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`ranking(student0)`', '`RA(prof0,student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`registration(course0,student0)`', '`intelligence(student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`registration(course0,student0)`', '`rating(course0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`salary(prof0,student0)`', '`capability(prof0,student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`salary(prof0,student0)`', '`RA(prof0,student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`sat(course0,student0)`', '`grade(course0,student0)`'),
	('`RA(prof0,student0),registration(course0,student0)`', '`sat(course0,student0)`', '`registration(course0,student0)`');
/*!40000 ALTER TABLE `Final_Path_BayesNets` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.FNodes
DROP TABLE IF EXISTS `FNodes`;
CREATE TABLE IF NOT EXISTS `FNodes` (
  `Fid` varchar(199) NOT NULL DEFAULT '',
  `FunctorName` varchar(64) DEFAULT NULL,
  `Type` varchar(5) DEFAULT NULL,
  `main` int(11) DEFAULT NULL,
  PRIMARY KEY (`Fid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.FNodes: ~12 rows (approximately)
DELETE FROM `FNodes`;
/*!40000 ALTER TABLE `FNodes` DISABLE KEYS */;
INSERT INTO `FNodes` (`Fid`, `FunctorName`, `Type`, `main`) VALUES
	('`a`', '`a`', 'Rnode', 1),
	('`b`', '`b`', 'Rnode', 1),
	('`capability(prof0,student0)`', 'capability', '2Node', 1),
	('`diff(course0)`', 'diff', '1Node', 1),
	('`grade(course0,student0)`', 'grade', '2Node', 1),
	('`intelligence(student0)`', 'intelligence', '1Node', 1),
	('`popularity(prof0)`', 'popularity', '1Node', 1),
	('`ranking(student0)`', 'ranking', '1Node', 1),
	('`rating(course0)`', 'rating', '1Node', 1),
	('`salary(prof0,student0)`', 'salary', '2Node', 1),
	('`sat(course0,student0)`', 'sat', '2Node', 1),
	('`teachingability(prof0)`', 'teachingability', '1Node', 1);
/*!40000 ALTER TABLE `FNodes` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.FNodes_Mapping
DROP TABLE IF EXISTS `FNodes_Mapping`;
CREATE TABLE IF NOT EXISTS `FNodes_Mapping` (
  `Fid` varchar(199) NOT NULL DEFAULT '',
  `FunctorName` varchar(64) DEFAULT NULL,
  `Type` varchar(5) DEFAULT NULL,
  `main` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.FNodes_Mapping: ~12 rows (approximately)
DELETE FROM `FNodes_Mapping`;
/*!40000 ALTER TABLE `FNodes_Mapping` DISABLE KEYS */;
INSERT INTO `FNodes_Mapping` (`Fid`, `FunctorName`, `Type`, `main`) VALUES
	('`RA(prof0,student0)`', '`a`', 'Rnode', 1),
	('`registration(course0,student0)`', '`b`', 'Rnode', 1),
	('`capability(prof0,student0)`', 'capability', '2Node', 1),
	('`diff(course0)`', 'diff', '1Node', 1),
	('`grade(course0,student0)`', 'grade', '2Node', 1),
	('`intelligence(student0)`', 'intelligence', '1Node', 1),
	('`popularity(prof0)`', 'popularity', '1Node', 1),
	('`ranking(student0)`', 'ranking', '1Node', 1),
	('`rating(course0)`', 'rating', '1Node', 1),
	('`salary(prof0,student0)`', 'salary', '2Node', 1),
	('`sat(course0,student0)`', 'sat', '2Node', 1),
	('`teachingability(prof0)`', 'teachingability', '1Node', 1);
/*!40000 ALTER TABLE `FNodes_Mapping` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.FNodes_pvars
DROP TABLE IF EXISTS `FNodes_pvars`;
CREATE TABLE IF NOT EXISTS `FNodes_pvars` (
  `Fid` varchar(199) NOT NULL DEFAULT '',
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.FNodes_pvars: ~14 rows (approximately)
DELETE FROM `FNodes_pvars`;
/*!40000 ALTER TABLE `FNodes_pvars` DISABLE KEYS */;
INSERT INTO `FNodes_pvars` (`Fid`, `pvid`) VALUES
	('`capability(prof0,student0)`', 'prof0'),
	('`grade(course0,student0)`', 'course0'),
	('`salary(prof0,student0)`', 'prof0'),
	('`sat(course0,student0)`', 'course0'),
	('`capability(prof0,student0)`', 'student0'),
	('`grade(course0,student0)`', 'student0'),
	('`salary(prof0,student0)`', 'student0'),
	('`sat(course0,student0)`', 'student0'),
	('`diff(course0)`', 'course0'),
	('`intelligence(student0)`', 'student0'),
	('`popularity(prof0)`', 'prof0'),
	('`ranking(student0)`', 'student0'),
	('`rating(course0)`', 'course0'),
	('`teachingability(prof0)`', 'prof0');
/*!40000 ALTER TABLE `FNodes_pvars` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.FNodes_pvars_UNION_RNodes_pvars
DROP TABLE IF EXISTS `FNodes_pvars_UNION_RNodes_pvars`;
CREATE TABLE IF NOT EXISTS `FNodes_pvars_UNION_RNodes_pvars` (
  `Fid` varchar(199) DEFAULT NULL,
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.FNodes_pvars_UNION_RNodes_pvars: ~18 rows (approximately)
DELETE FROM `FNodes_pvars_UNION_RNodes_pvars`;
/*!40000 ALTER TABLE `FNodes_pvars_UNION_RNodes_pvars` DISABLE KEYS */;
INSERT INTO `FNodes_pvars_UNION_RNodes_pvars` (`Fid`, `pvid`) VALUES
	('`a`', 'prof0'),
	('`b`', 'course0'),
	('`a`', 'student0'),
	('`b`', 'student0'),
	('`capability(prof0,student0)`', 'prof0'),
	('`grade(course0,student0)`', 'course0'),
	('`salary(prof0,student0)`', 'prof0'),
	('`sat(course0,student0)`', 'course0'),
	('`capability(prof0,student0)`', 'student0'),
	('`grade(course0,student0)`', 'student0'),
	('`salary(prof0,student0)`', 'student0'),
	('`sat(course0,student0)`', 'student0'),
	('`diff(course0)`', 'course0'),
	('`intelligence(student0)`', 'student0'),
	('`popularity(prof0)`', 'prof0'),
	('`ranking(student0)`', 'student0'),
	('`rating(course0)`', 'course0'),
	('`teachingability(prof0)`', 'prof0');
/*!40000 ALTER TABLE `FNodes_pvars_UNION_RNodes_pvars` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ForeignKeyColumns
DROP TABLE IF EXISTS `ForeignKeyColumns`;
CREATE TABLE IF NOT EXISTS `ForeignKeyColumns` (
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `COLUMN_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `REFERENCED_TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `REFERENCED_COLUMN_NAME` varchar(64) CHARACTER SET utf8 DEFAULT NULL,
  `CONSTRAINT_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `ORDINAL_POSITION` bigint(21) unsigned NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ForeignKeyColumns: ~4 rows (approximately)
DELETE FROM `ForeignKeyColumns`;
/*!40000 ALTER TABLE `ForeignKeyColumns` DISABLE KEYS */;
INSERT INTO `ForeignKeyColumns` (`TABLE_NAME`, `COLUMN_NAME`, `REFERENCED_TABLE_NAME`, `REFERENCED_COLUMN_NAME`, `CONSTRAINT_NAME`, `ORDINAL_POSITION`) VALUES
	('RA', 'prof_id', 'prof', 'prof_id', 'FK_RA_2', 2),
	('RA', 'student_id', 'student', 'student_id', 'FK_RA_1', 3),
	('registration', 'course_id', 'course', 'course_id', 'FK_registration_2', 1),
	('registration', 'student_id', 'student', 'student_id', 'FK_registration_1', 2);
/*!40000 ALTER TABLE `ForeignKeyColumns` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ForeignKeys_pvars
DROP TABLE IF EXISTS `ForeignKeys_pvars`;
CREATE TABLE IF NOT EXISTS `ForeignKeys_pvars` (
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `REFERENCED_TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `COLUMN_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `index_number` bigint(20) NOT NULL DEFAULT '0',
  `ARGUMENT_POSITION` bigint(21) unsigned NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ForeignKeys_pvars: ~4 rows (approximately)
DELETE FROM `ForeignKeys_pvars`;
/*!40000 ALTER TABLE `ForeignKeys_pvars` DISABLE KEYS */;
INSERT INTO `ForeignKeys_pvars` (`TABLE_NAME`, `REFERENCED_TABLE_NAME`, `COLUMN_NAME`, `pvid`, `index_number`, `ARGUMENT_POSITION`) VALUES
	('RA', 'prof', 'prof_id', 'prof0', 0, 2),
	('RA', 'student', 'student_id', 'student0', 0, 3),
	('registration', 'course', 'course_id', 'course0', 0, 1),
	('registration', 'student', 'student_id', 'student0', 0, 2);
/*!40000 ALTER TABLE `ForeignKeys_pvars` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.grade(course0,student0)_CP
DROP TABLE IF EXISTS `grade(course0,student0)_CP`;
CREATE TABLE IF NOT EXISTS `grade(course0,student0)_CP` (
  `grade(course0,student0)` varchar(45) DEFAULT NULL,
  `registration(course0,student0)` varchar(5) DEFAULT NULL,
  `intelligence(student0)` varchar(45) DEFAULT NULL,
  `ParentSum` bigint(20) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL,
  KEY `grade(course0,student0)_CP` (`grade(course0,student0)`,`registration(course0,student0)`,`intelligence(student0)`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.grade(course0,student0)_CP: ~11 rows (approximately)
DELETE FROM `grade(course0,student0)_CP`;
/*!40000 ALTER TABLE `grade(course0,student0)_CP` DISABLE KEYS */;
INSERT INTO `grade(course0,student0)_CP` (`grade(course0,student0)`, `registration(course0,student0)`, `intelligence(student0)`, `ParentSum`, `local_mult`, `CP`, `likelihood`, `prior`) VALUES
	('1', 'T', '2', 180, 7, 0.233333, -10.19, 0.084211),
	('1', 'T', '3', 216, 25, 0.694444, -9.12, 0.084211),
	('2', 'T', '1', 156, 1, 0.038462, -3.26, 0.076316),
	('2', 'T', '2', 180, 17, 0.566667, -9.66, 0.076316),
	('2', 'T', '3', 216, 11, 0.305556, -13.04, 0.076316),
	('3', 'T', '1', 156, 12, 0.461538, -9.28, 0.047368),
	('3', 'T', '2', 180, 6, 0.200000, -9.66, 0.047368),
	('4', 'T', '1', 156, 13, 0.500000, -9.01, 0.034211),
	('N/A', 'F', '1', 684, 114, 1.000000, 0.00, 0.757895),
	('N/A', 'F', '2', 600, 100, 1.000000, 0.00, 0.757895),
	('N/A', 'F', '3', 444, 74, 1.000000, 0.00, 0.757895);
/*!40000 ALTER TABLE `grade(course0,student0)_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.InheritedEdges
DROP TABLE IF EXISTS `InheritedEdges`;
CREATE TABLE IF NOT EXISTS `InheritedEdges` (
  `Rchain` varchar(256) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(197) NOT NULL,
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.InheritedEdges: ~12 rows (approximately)
DELETE FROM `InheritedEdges`;
/*!40000 ALTER TABLE `InheritedEdges` DISABLE KEYS */;
INSERT INTO `InheritedEdges` (`Rchain`, `child`, `parent`) VALUES
	('`a,b`', '`capability(prof0,student0)`', '`a`'),
	('`a,b`', '`diff(course0)`', '`grade(course0,student0)`'),
	('`a,b`', '`grade(course0,student0)`', '`b`'),
	('`a,b`', '`grade(course0,student0)`', '`intelligence(student0)`'),
	('`a,b`', '`intelligence(student0)`', '`a`'),
	('`a,b`', '`popularity(prof0)`', '`a`'),
	('`a,b`', '`popularity(prof0)`', '`teachingability(prof0)`'),
	('`a,b`', '`ranking(student0)`', '`intelligence(student0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`a`'),
	('`a,b`', '`salary(prof0,student0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`sat(course0,student0)`', '`b`'),
	('`a,b`', '`sat(course0,student0)`', '`grade(course0,student0)`');
/*!40000 ALTER TABLE `InheritedEdges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.InputColumns
DROP TABLE IF EXISTS `InputColumns`;
CREATE TABLE IF NOT EXISTS `InputColumns` (
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `COLUMN_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `REFERENCED_TABLE_NAME` varchar(64) CHARACTER SET utf8 DEFAULT NULL,
  `REFERENCED_COLUMN_NAME` varchar(64) CHARACTER SET utf8 DEFAULT NULL,
  `CONSTRAINT_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `ORDINAL_POSITION` bigint(21) unsigned NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.InputColumns: ~7 rows (approximately)
DELETE FROM `InputColumns`;
/*!40000 ALTER TABLE `InputColumns` DISABLE KEYS */;
INSERT INTO `InputColumns` (`TABLE_NAME`, `COLUMN_NAME`, `REFERENCED_TABLE_NAME`, `REFERENCED_COLUMN_NAME`, `CONSTRAINT_NAME`, `ORDINAL_POSITION`) VALUES
	('course', 'course_id', NULL, NULL, 'PRIMARY', 1),
	('prof', 'prof_id', NULL, NULL, 'PRIMARY', 1),
	('RA', 'prof_id', NULL, NULL, 'PRIMARY', 2),
	('RA', 'student_id', NULL, NULL, 'PRIMARY', 3),
	('registration', 'course_id', NULL, NULL, 'PRIMARY', 1),
	('registration', 'student_id', NULL, NULL, 'PRIMARY', 2),
	('student', 'student_id', NULL, NULL, 'PRIMARY', 1);
/*!40000 ALTER TABLE `InputColumns` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.intelligence(student0)_CP
DROP TABLE IF EXISTS `intelligence(student0)_CP`;
CREATE TABLE IF NOT EXISTS `intelligence(student0)_CP` (
  `intelligence(student0)` varchar(45) DEFAULT NULL,
  `RA(prof0,student0)` varchar(5) DEFAULT NULL,
  `ParentSum` bigint(20) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL,
  KEY `intelligence(student0)_CP` (`intelligence(student0)`,`RA(prof0,student0)`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.intelligence(student0)_CP: ~6 rows (approximately)
DELETE FROM `intelligence(student0)_CP`;
/*!40000 ALTER TABLE `intelligence(student0)_CP` DISABLE KEYS */;
INSERT INTO `intelligence(student0)_CP` (`intelligence(student0)`, `RA(prof0,student0)`, `ParentSum`, `local_mult`, `CP`, `likelihood`, `prior`) VALUES
	('1', 'F', 2030, 80, 0.394089, -74.49, 0.368421),
	('1', 'T', 250, 4, 0.160000, -7.33, 0.368421),
	('2', 'F', 2030, 65, 0.320197, -74.02, 0.342105),
	('2', 'T', 250, 13, 0.520000, -8.50, 0.342105),
	('3', 'F', 2030, 58, 0.285714, -72.66, 0.289474),
	('3', 'T', 250, 8, 0.320000, -9.12, 0.289474);
/*!40000 ALTER TABLE `intelligence(student0)_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Knowledge_Forbidden_Edges
DROP TABLE IF EXISTS `Knowledge_Forbidden_Edges`;
CREATE TABLE IF NOT EXISTS `Knowledge_Forbidden_Edges` (
  `Rchain` varchar(256) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(197) NOT NULL,
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Knowledge_Forbidden_Edges: ~0 rows (approximately)
DELETE FROM `Knowledge_Forbidden_Edges`;
/*!40000 ALTER TABLE `Knowledge_Forbidden_Edges` DISABLE KEYS */;
/*!40000 ALTER TABLE `Knowledge_Forbidden_Edges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Knowledge_Required_Edges
DROP TABLE IF EXISTS `Knowledge_Required_Edges`;
CREATE TABLE IF NOT EXISTS `Knowledge_Required_Edges` (
  `Rchain` varchar(256) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(197) NOT NULL,
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Knowledge_Required_Edges: ~0 rows (approximately)
DELETE FROM `Knowledge_Required_Edges`;
/*!40000 ALTER TABLE `Knowledge_Required_Edges` DISABLE KEYS */;
/*!40000 ALTER TABLE `Knowledge_Required_Edges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.lattice_mapping
DROP TABLE IF EXISTS `lattice_mapping`;
CREATE TABLE IF NOT EXISTS `lattice_mapping` (
  `orig_rnid` varchar(200) NOT NULL DEFAULT '',
  `rnid` varchar(20) NOT NULL DEFAULT '',
  PRIMARY KEY (`orig_rnid`,`rnid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.lattice_mapping: ~3 rows (approximately)
DELETE FROM `lattice_mapping`;
/*!40000 ALTER TABLE `lattice_mapping` DISABLE KEYS */;
INSERT INTO `lattice_mapping` (`orig_rnid`, `rnid`) VALUES
	('`RA(prof0,student0),registration(course0,student0)`', '`a,b`'),
	('`RA(prof0,student0)`', '`a`'),
	('`registration(course0,student0)`', '`b`');
/*!40000 ALTER TABLE `lattice_mapping` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.lattice_membership
DROP TABLE IF EXISTS `lattice_membership`;
CREATE TABLE IF NOT EXISTS `lattice_membership` (
  `name` varchar(20) NOT NULL DEFAULT '',
  `member` varchar(20) NOT NULL DEFAULT '',
  PRIMARY KEY (`name`,`member`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.lattice_membership: ~4 rows (approximately)
DELETE FROM `lattice_membership`;
/*!40000 ALTER TABLE `lattice_membership` DISABLE KEYS */;
INSERT INTO `lattice_membership` (`name`, `member`) VALUES
	('`a,b`', '`a`'),
	('`a,b`', '`b`'),
	('`a`', '`a`'),
	('`b`', '`b`');
/*!40000 ALTER TABLE `lattice_membership` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.lattice_rel
DROP TABLE IF EXISTS `lattice_rel`;
CREATE TABLE IF NOT EXISTS `lattice_rel` (
  `parent` varchar(20) NOT NULL DEFAULT '',
  `child` varchar(20) NOT NULL DEFAULT '',
  `removed` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`parent`,`child`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.lattice_rel: ~4 rows (approximately)
DELETE FROM `lattice_rel`;
/*!40000 ALTER TABLE `lattice_rel` DISABLE KEYS */;
INSERT INTO `lattice_rel` (`parent`, `child`, `removed`) VALUES
	('EmptySet', '`a`', '`a`'),
	('EmptySet', '`b`', '`b`'),
	('`a`', '`a,b`', '`b`'),
	('`b`', '`a,b`', '`a`');
/*!40000 ALTER TABLE `lattice_rel` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.lattice_set
DROP TABLE IF EXISTS `lattice_set`;
CREATE TABLE IF NOT EXISTS `lattice_set` (
  `name` varchar(20) NOT NULL DEFAULT '',
  `length` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`name`,`length`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.lattice_set: ~3 rows (approximately)
DELETE FROM `lattice_set`;
/*!40000 ALTER TABLE `lattice_set` DISABLE KEYS */;
INSERT INTO `lattice_set` (`name`, `length`) VALUES
	('`a,b`', 2),
	('`a`', 1),
	('`b`', 1);
/*!40000 ALTER TABLE `lattice_set` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.LearnedEdges
DROP TABLE IF EXISTS `LearnedEdges`;
CREATE TABLE IF NOT EXISTS `LearnedEdges` (
  `Rchain` varchar(256) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(197) NOT NULL,
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.LearnedEdges: ~0 rows (approximately)
DELETE FROM `LearnedEdges`;
/*!40000 ALTER TABLE `LearnedEdges` DISABLE KEYS */;
/*!40000 ALTER TABLE `LearnedEdges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.NewLearnedEdges
DROP TABLE IF EXISTS `NewLearnedEdges`;
CREATE TABLE IF NOT EXISTS `NewLearnedEdges` (
  `Rchain` varchar(255) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(197) NOT NULL,
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.NewLearnedEdges: ~0 rows (approximately)
DELETE FROM `NewLearnedEdges`;
/*!40000 ALTER TABLE `NewLearnedEdges` DISABLE KEYS */;
/*!40000 ALTER TABLE `NewLearnedEdges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.NoPKeys
DROP TABLE IF EXISTS `NoPKeys`;
CREATE TABLE IF NOT EXISTS `NoPKeys` (
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.NoPKeys: ~0 rows (approximately)
DELETE FROM `NoPKeys`;
/*!40000 ALTER TABLE `NoPKeys` DISABLE KEYS */;
/*!40000 ALTER TABLE `NoPKeys` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.NumAttributes
DROP TABLE IF EXISTS `NumAttributes`;
CREATE TABLE IF NOT EXISTS `NumAttributes` (
  `NumAtts` bigint(21) NOT NULL DEFAULT '0',
  `COLUMN_NAME` varchar(30) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.NumAttributes: ~12 rows (approximately)
DELETE FROM `NumAttributes`;
/*!40000 ALTER TABLE `NumAttributes` DISABLE KEYS */;
INSERT INTO `NumAttributes` (`NumAtts`, `COLUMN_NAME`) VALUES
	(5, 'capability'),
	(2, 'diff'),
	(4, 'grade'),
	(3, 'intelligence'),
	(2, 'popularity'),
	(5, 'ranking'),
	(2, 'rating'),
	(3, 'salary'),
	(3, 'sat'),
	(2, 'teachingability'),
	(2, '`a`'),
	(2, '`b`');
/*!40000 ALTER TABLE `NumAttributes` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Path_Aux_Edges
DROP TABLE IF EXISTS `Path_Aux_Edges`;
CREATE TABLE IF NOT EXISTS `Path_Aux_Edges` (
  `Rchain` varchar(20) NOT NULL DEFAULT '',
  `child` varchar(199) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `parent` varchar(199) CHARACTER SET utf8 NOT NULL DEFAULT '',
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Path_Aux_Edges: ~0 rows (approximately)
DELETE FROM `Path_Aux_Edges`;
/*!40000 ALTER TABLE `Path_Aux_Edges` DISABLE KEYS */;
/*!40000 ALTER TABLE `Path_Aux_Edges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Path_BayesNets
DROP TABLE IF EXISTS `Path_BayesNets`;
CREATE TABLE IF NOT EXISTS `Path_BayesNets` (
  `Rchain` varchar(256) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(197) NOT NULL,
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Path_BayesNets: ~38 rows (approximately)
DELETE FROM `Path_BayesNets`;
/*!40000 ALTER TABLE `Path_BayesNets` DISABLE KEYS */;
INSERT INTO `Path_BayesNets` (`Rchain`, `child`, `parent`) VALUES
	('`a,b`', '`a`', ''),
	('`a,b`', '`a`', '`teachingability(prof0)`'),
	('`a,b`', '`b`', ''),
	('`a,b`', '`b`', '`intelligence(student0)`'),
	('`a,b`', '`b`', '`rating(course0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`a`'),
	('`a,b`', '`diff(course0)`', '`grade(course0,student0)`'),
	('`a,b`', '`grade(course0,student0)`', '`b`'),
	('`a,b`', '`grade(course0,student0)`', '`intelligence(student0)`'),
	('`a,b`', '`intelligence(student0)`', '`a`'),
	('`a,b`', '`popularity(prof0)`', '`a`'),
	('`a,b`', '`popularity(prof0)`', '`teachingability(prof0)`'),
	('`a,b`', '`ranking(student0)`', '`a`'),
	('`a,b`', '`ranking(student0)`', '`intelligence(student0)`'),
	('`a,b`', '`rating(course0)`', ''),
	('`a,b`', '`salary(prof0,student0)`', '`a`'),
	('`a,b`', '`salary(prof0,student0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`sat(course0,student0)`', '`b`'),
	('`a,b`', '`sat(course0,student0)`', '`grade(course0,student0)`'),
	('`a,b`', '`teachingability(prof0)`', ''),
	('`a`', '`a`', ''),
	('`a`', '`capability(prof0,student0)`', '`a`'),
	('`a`', '`intelligence(student0)`', '`a`'),
	('`a`', '`popularity(prof0)`', '`a`'),
	('`a`', '`popularity(prof0)`', '`teachingability(prof0)`'),
	('`a`', '`ranking(student0)`', '`intelligence(student0)`'),
	('`a`', '`salary(prof0,student0)`', '`a`'),
	('`a`', '`salary(prof0,student0)`', '`capability(prof0,student0)`'),
	('`a`', '`teachingability(prof0)`', ''),
	('`b`', '`b`', ''),
	('`b`', '`diff(course0)`', '`grade(course0,student0)`'),
	('`b`', '`grade(course0,student0)`', '`b`'),
	('`b`', '`grade(course0,student0)`', '`intelligence(student0)`'),
	('`b`', '`intelligence(student0)`', ''),
	('`b`', '`ranking(student0)`', '`intelligence(student0)`'),
	('`b`', '`rating(course0)`', ''),
	('`b`', '`sat(course0,student0)`', '`b`'),
	('`b`', '`sat(course0,student0)`', '`grade(course0,student0)`');
/*!40000 ALTER TABLE `Path_BayesNets` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Path_BN_nodes
DROP TABLE IF EXISTS `Path_BN_nodes`;
CREATE TABLE IF NOT EXISTS `Path_BN_nodes` (
  `Rchain` varchar(20) NOT NULL DEFAULT '',
  `node` varchar(199) CHARACTER SET utf8 NOT NULL DEFAULT '',
  KEY `HashIndex` (`Rchain`,`node`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Path_BN_nodes: ~22 rows (approximately)
DELETE FROM `Path_BN_nodes`;
/*!40000 ALTER TABLE `Path_BN_nodes` DISABLE KEYS */;
INSERT INTO `Path_BN_nodes` (`Rchain`, `node`) VALUES
	('`a,b`', '`capability(prof0,student0)`'),
	('`a,b`', '`diff(course0)`'),
	('`a,b`', '`grade(course0,student0)`'),
	('`a,b`', '`intelligence(student0)`'),
	('`a,b`', '`popularity(prof0)`'),
	('`a,b`', '`ranking(student0)`'),
	('`a,b`', '`rating(course0)`'),
	('`a,b`', '`salary(prof0,student0)`'),
	('`a,b`', '`sat(course0,student0)`'),
	('`a,b`', '`teachingability(prof0)`'),
	('`a`', '`capability(prof0,student0)`'),
	('`a`', '`intelligence(student0)`'),
	('`a`', '`popularity(prof0)`'),
	('`a`', '`ranking(student0)`'),
	('`a`', '`salary(prof0,student0)`'),
	('`a`', '`teachingability(prof0)`'),
	('`b`', '`diff(course0)`'),
	('`b`', '`grade(course0,student0)`'),
	('`b`', '`intelligence(student0)`'),
	('`b`', '`ranking(student0)`'),
	('`b`', '`rating(course0)`'),
	('`b`', '`sat(course0,student0)`');
/*!40000 ALTER TABLE `Path_BN_nodes` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Path_Complement_Edges
DROP TABLE IF EXISTS `Path_Complement_Edges`;
CREATE TABLE IF NOT EXISTS `Path_Complement_Edges` (
  `Rchain` varchar(256) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(197) NOT NULL,
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Path_Complement_Edges: ~159 rows (approximately)
DELETE FROM `Path_Complement_Edges`;
/*!40000 ALTER TABLE `Path_Complement_Edges` DISABLE KEYS */;
INSERT INTO `Path_Complement_Edges` (`Rchain`, `child`, `parent`) VALUES
	('`a,b`', '`capability(prof0,student0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`diff(course0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`grade(course0,student0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`intelligence(student0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`popularity(prof0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`ranking(student0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`rating(course0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`sat(course0,student0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`teachingability(prof0)`'),
	('`a,b`', '`diff(course0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`diff(course0)`', '`diff(course0)`'),
	('`a,b`', '`diff(course0)`', '`intelligence(student0)`'),
	('`a,b`', '`diff(course0)`', '`popularity(prof0)`'),
	('`a,b`', '`diff(course0)`', '`ranking(student0)`'),
	('`a,b`', '`diff(course0)`', '`rating(course0)`'),
	('`a,b`', '`diff(course0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`diff(course0)`', '`sat(course0,student0)`'),
	('`a,b`', '`diff(course0)`', '`teachingability(prof0)`'),
	('`a,b`', '`grade(course0,student0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`grade(course0,student0)`', '`diff(course0)`'),
	('`a,b`', '`grade(course0,student0)`', '`grade(course0,student0)`'),
	('`a,b`', '`grade(course0,student0)`', '`popularity(prof0)`'),
	('`a,b`', '`grade(course0,student0)`', '`ranking(student0)`'),
	('`a,b`', '`grade(course0,student0)`', '`rating(course0)`'),
	('`a,b`', '`grade(course0,student0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`grade(course0,student0)`', '`sat(course0,student0)`'),
	('`a,b`', '`grade(course0,student0)`', '`teachingability(prof0)`'),
	('`a,b`', '`intelligence(student0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`intelligence(student0)`', '`diff(course0)`'),
	('`a,b`', '`intelligence(student0)`', '`grade(course0,student0)`'),
	('`a,b`', '`intelligence(student0)`', '`intelligence(student0)`'),
	('`a,b`', '`intelligence(student0)`', '`popularity(prof0)`'),
	('`a,b`', '`intelligence(student0)`', '`ranking(student0)`'),
	('`a,b`', '`intelligence(student0)`', '`rating(course0)`'),
	('`a,b`', '`intelligence(student0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`intelligence(student0)`', '`sat(course0,student0)`'),
	('`a,b`', '`intelligence(student0)`', '`teachingability(prof0)`'),
	('`a,b`', '`popularity(prof0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`popularity(prof0)`', '`diff(course0)`'),
	('`a,b`', '`popularity(prof0)`', '`grade(course0,student0)`'),
	('`a,b`', '`popularity(prof0)`', '`intelligence(student0)`'),
	('`a,b`', '`popularity(prof0)`', '`popularity(prof0)`'),
	('`a,b`', '`popularity(prof0)`', '`ranking(student0)`'),
	('`a,b`', '`popularity(prof0)`', '`rating(course0)`'),
	('`a,b`', '`popularity(prof0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`popularity(prof0)`', '`sat(course0,student0)`'),
	('`a,b`', '`ranking(student0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`ranking(student0)`', '`diff(course0)`'),
	('`a,b`', '`ranking(student0)`', '`grade(course0,student0)`'),
	('`a,b`', '`ranking(student0)`', '`popularity(prof0)`'),
	('`a,b`', '`ranking(student0)`', '`ranking(student0)`'),
	('`a,b`', '`ranking(student0)`', '`rating(course0)`'),
	('`a,b`', '`ranking(student0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`ranking(student0)`', '`sat(course0,student0)`'),
	('`a,b`', '`ranking(student0)`', '`teachingability(prof0)`'),
	('`a,b`', '`rating(course0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`rating(course0)`', '`diff(course0)`'),
	('`a,b`', '`rating(course0)`', '`grade(course0,student0)`'),
	('`a,b`', '`rating(course0)`', '`intelligence(student0)`'),
	('`a,b`', '`rating(course0)`', '`popularity(prof0)`'),
	('`a,b`', '`rating(course0)`', '`ranking(student0)`'),
	('`a,b`', '`rating(course0)`', '`rating(course0)`'),
	('`a,b`', '`rating(course0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`rating(course0)`', '`sat(course0,student0)`'),
	('`a,b`', '`rating(course0)`', '`teachingability(prof0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`diff(course0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`grade(course0,student0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`intelligence(student0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`popularity(prof0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`ranking(student0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`rating(course0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`sat(course0,student0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`teachingability(prof0)`'),
	('`a,b`', '`sat(course0,student0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`sat(course0,student0)`', '`diff(course0)`'),
	('`a,b`', '`sat(course0,student0)`', '`intelligence(student0)`'),
	('`a,b`', '`sat(course0,student0)`', '`popularity(prof0)`'),
	('`a,b`', '`sat(course0,student0)`', '`ranking(student0)`'),
	('`a,b`', '`sat(course0,student0)`', '`rating(course0)`'),
	('`a,b`', '`sat(course0,student0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`sat(course0,student0)`', '`sat(course0,student0)`'),
	('`a,b`', '`sat(course0,student0)`', '`teachingability(prof0)`'),
	('`a,b`', '`teachingability(prof0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`teachingability(prof0)`', '`diff(course0)`'),
	('`a,b`', '`teachingability(prof0)`', '`grade(course0,student0)`'),
	('`a,b`', '`teachingability(prof0)`', '`intelligence(student0)`'),
	('`a,b`', '`teachingability(prof0)`', '`popularity(prof0)`'),
	('`a,b`', '`teachingability(prof0)`', '`ranking(student0)`'),
	('`a,b`', '`teachingability(prof0)`', '`rating(course0)`'),
	('`a,b`', '`teachingability(prof0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`teachingability(prof0)`', '`sat(course0,student0)`'),
	('`a,b`', '`teachingability(prof0)`', '`teachingability(prof0)`'),
	('`a`', '`capability(prof0,student0)`', '`capability(prof0,student0)`'),
	('`a`', '`capability(prof0,student0)`', '`intelligence(student0)`'),
	('`a`', '`capability(prof0,student0)`', '`popularity(prof0)`'),
	('`a`', '`capability(prof0,student0)`', '`ranking(student0)`'),
	('`a`', '`capability(prof0,student0)`', '`salary(prof0,student0)`'),
	('`a`', '`capability(prof0,student0)`', '`teachingability(prof0)`'),
	('`a`', '`intelligence(student0)`', '`capability(prof0,student0)`'),
	('`a`', '`intelligence(student0)`', '`intelligence(student0)`'),
	('`a`', '`intelligence(student0)`', '`popularity(prof0)`'),
	('`a`', '`intelligence(student0)`', '`ranking(student0)`'),
	('`a`', '`intelligence(student0)`', '`salary(prof0,student0)`'),
	('`a`', '`intelligence(student0)`', '`teachingability(prof0)`'),
	('`a`', '`popularity(prof0)`', '`capability(prof0,student0)`'),
	('`a`', '`popularity(prof0)`', '`intelligence(student0)`'),
	('`a`', '`popularity(prof0)`', '`popularity(prof0)`'),
	('`a`', '`popularity(prof0)`', '`ranking(student0)`'),
	('`a`', '`popularity(prof0)`', '`salary(prof0,student0)`'),
	('`a`', '`ranking(student0)`', '`capability(prof0,student0)`'),
	('`a`', '`ranking(student0)`', '`popularity(prof0)`'),
	('`a`', '`ranking(student0)`', '`ranking(student0)`'),
	('`a`', '`ranking(student0)`', '`salary(prof0,student0)`'),
	('`a`', '`ranking(student0)`', '`teachingability(prof0)`'),
	('`a`', '`salary(prof0,student0)`', '`intelligence(student0)`'),
	('`a`', '`salary(prof0,student0)`', '`popularity(prof0)`'),
	('`a`', '`salary(prof0,student0)`', '`ranking(student0)`'),
	('`a`', '`salary(prof0,student0)`', '`salary(prof0,student0)`'),
	('`a`', '`salary(prof0,student0)`', '`teachingability(prof0)`'),
	('`a`', '`teachingability(prof0)`', '`capability(prof0,student0)`'),
	('`a`', '`teachingability(prof0)`', '`intelligence(student0)`'),
	('`a`', '`teachingability(prof0)`', '`popularity(prof0)`'),
	('`a`', '`teachingability(prof0)`', '`ranking(student0)`'),
	('`a`', '`teachingability(prof0)`', '`salary(prof0,student0)`'),
	('`a`', '`teachingability(prof0)`', '`teachingability(prof0)`'),
	('`b`', '`diff(course0)`', '`diff(course0)`'),
	('`b`', '`diff(course0)`', '`intelligence(student0)`'),
	('`b`', '`diff(course0)`', '`ranking(student0)`'),
	('`b`', '`diff(course0)`', '`rating(course0)`'),
	('`b`', '`diff(course0)`', '`sat(course0,student0)`'),
	('`b`', '`grade(course0,student0)`', '`diff(course0)`'),
	('`b`', '`grade(course0,student0)`', '`grade(course0,student0)`'),
	('`b`', '`grade(course0,student0)`', '`ranking(student0)`'),
	('`b`', '`grade(course0,student0)`', '`rating(course0)`'),
	('`b`', '`grade(course0,student0)`', '`sat(course0,student0)`'),
	('`b`', '`intelligence(student0)`', '`diff(course0)`'),
	('`b`', '`intelligence(student0)`', '`grade(course0,student0)`'),
	('`b`', '`intelligence(student0)`', '`intelligence(student0)`'),
	('`b`', '`intelligence(student0)`', '`ranking(student0)`'),
	('`b`', '`intelligence(student0)`', '`rating(course0)`'),
	('`b`', '`intelligence(student0)`', '`sat(course0,student0)`'),
	('`b`', '`ranking(student0)`', '`diff(course0)`'),
	('`b`', '`ranking(student0)`', '`grade(course0,student0)`'),
	('`b`', '`ranking(student0)`', '`ranking(student0)`'),
	('`b`', '`ranking(student0)`', '`rating(course0)`'),
	('`b`', '`ranking(student0)`', '`sat(course0,student0)`'),
	('`b`', '`rating(course0)`', '`diff(course0)`'),
	('`b`', '`rating(course0)`', '`grade(course0,student0)`'),
	('`b`', '`rating(course0)`', '`intelligence(student0)`'),
	('`b`', '`rating(course0)`', '`ranking(student0)`'),
	('`b`', '`rating(course0)`', '`rating(course0)`'),
	('`b`', '`rating(course0)`', '`sat(course0,student0)`'),
	('`b`', '`sat(course0,student0)`', '`diff(course0)`'),
	('`b`', '`sat(course0,student0)`', '`intelligence(student0)`'),
	('`b`', '`sat(course0,student0)`', '`ranking(student0)`'),
	('`b`', '`sat(course0,student0)`', '`rating(course0)`'),
	('`b`', '`sat(course0,student0)`', '`sat(course0,student0)`');
/*!40000 ALTER TABLE `Path_Complement_Edges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Path_Forbidden_Edges
DROP TABLE IF EXISTS `Path_Forbidden_Edges`;
CREATE TABLE IF NOT EXISTS `Path_Forbidden_Edges` (
  `Rchain` varchar(256) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(197) NOT NULL,
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Path_Forbidden_Edges: ~75 rows (approximately)
DELETE FROM `Path_Forbidden_Edges`;
/*!40000 ALTER TABLE `Path_Forbidden_Edges` DISABLE KEYS */;
INSERT INTO `Path_Forbidden_Edges` (`Rchain`, `child`, `parent`) VALUES
	('`a,b`', '`capability(prof0,student0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`intelligence(student0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`popularity(prof0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`ranking(student0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`capability(prof0,student0)`', '`teachingability(prof0)`'),
	('`a,b`', '`diff(course0)`', '`diff(course0)`'),
	('`a,b`', '`diff(course0)`', '`intelligence(student0)`'),
	('`a,b`', '`diff(course0)`', '`ranking(student0)`'),
	('`a,b`', '`diff(course0)`', '`rating(course0)`'),
	('`a,b`', '`diff(course0)`', '`sat(course0,student0)`'),
	('`a,b`', '`grade(course0,student0)`', '`diff(course0)`'),
	('`a,b`', '`grade(course0,student0)`', '`grade(course0,student0)`'),
	('`a,b`', '`grade(course0,student0)`', '`ranking(student0)`'),
	('`a,b`', '`grade(course0,student0)`', '`rating(course0)`'),
	('`a,b`', '`grade(course0,student0)`', '`sat(course0,student0)`'),
	('`a,b`', '`intelligence(student0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`intelligence(student0)`', '`diff(course0)`'),
	('`a,b`', '`intelligence(student0)`', '`grade(course0,student0)`'),
	('`a,b`', '`intelligence(student0)`', '`intelligence(student0)`'),
	('`a,b`', '`intelligence(student0)`', '`popularity(prof0)`'),
	('`a,b`', '`intelligence(student0)`', '`ranking(student0)`'),
	('`a,b`', '`intelligence(student0)`', '`rating(course0)`'),
	('`a,b`', '`intelligence(student0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`intelligence(student0)`', '`sat(course0,student0)`'),
	('`a,b`', '`intelligence(student0)`', '`teachingability(prof0)`'),
	('`a,b`', '`popularity(prof0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`popularity(prof0)`', '`intelligence(student0)`'),
	('`a,b`', '`popularity(prof0)`', '`popularity(prof0)`'),
	('`a,b`', '`popularity(prof0)`', '`ranking(student0)`'),
	('`a,b`', '`popularity(prof0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`ranking(student0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`ranking(student0)`', '`diff(course0)`'),
	('`a,b`', '`ranking(student0)`', '`grade(course0,student0)`'),
	('`a,b`', '`ranking(student0)`', '`popularity(prof0)`'),
	('`a,b`', '`ranking(student0)`', '`ranking(student0)`'),
	('`a,b`', '`ranking(student0)`', '`rating(course0)`'),
	('`a,b`', '`ranking(student0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`ranking(student0)`', '`sat(course0,student0)`'),
	('`a,b`', '`ranking(student0)`', '`teachingability(prof0)`'),
	('`a,b`', '`rating(course0)`', '`diff(course0)`'),
	('`a,b`', '`rating(course0)`', '`grade(course0,student0)`'),
	('`a,b`', '`rating(course0)`', '`intelligence(student0)`'),
	('`a,b`', '`rating(course0)`', '`ranking(student0)`'),
	('`a,b`', '`rating(course0)`', '`rating(course0)`'),
	('`a,b`', '`rating(course0)`', '`sat(course0,student0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`intelligence(student0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`popularity(prof0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`ranking(student0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`teachingability(prof0)`'),
	('`a,b`', '`sat(course0,student0)`', '`diff(course0)`'),
	('`a,b`', '`sat(course0,student0)`', '`intelligence(student0)`'),
	('`a,b`', '`sat(course0,student0)`', '`ranking(student0)`'),
	('`a,b`', '`sat(course0,student0)`', '`rating(course0)`'),
	('`a,b`', '`sat(course0,student0)`', '`sat(course0,student0)`'),
	('`a,b`', '`teachingability(prof0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`teachingability(prof0)`', '`intelligence(student0)`'),
	('`a,b`', '`teachingability(prof0)`', '`popularity(prof0)`'),
	('`a,b`', '`teachingability(prof0)`', '`ranking(student0)`'),
	('`a,b`', '`teachingability(prof0)`', '`salary(prof0,student0)`'),
	('`a,b`', '`teachingability(prof0)`', '`teachingability(prof0)`'),
	('`a`', '`intelligence(student0)`', '`intelligence(student0)`'),
	('`a`', '`intelligence(student0)`', '`ranking(student0)`'),
	('`a`', '`popularity(prof0)`', '`popularity(prof0)`'),
	('`a`', '`ranking(student0)`', '`ranking(student0)`'),
	('`a`', '`teachingability(prof0)`', '`popularity(prof0)`'),
	('`a`', '`teachingability(prof0)`', '`teachingability(prof0)`'),
	('`b`', '`diff(course0)`', '`diff(course0)`'),
	('`b`', '`diff(course0)`', '`rating(course0)`'),
	('`b`', '`intelligence(student0)`', '`intelligence(student0)`'),
	('`b`', '`intelligence(student0)`', '`ranking(student0)`'),
	('`b`', '`ranking(student0)`', '`ranking(student0)`'),
	('`b`', '`rating(course0)`', '`diff(course0)`'),
	('`b`', '`rating(course0)`', '`rating(course0)`');
/*!40000 ALTER TABLE `Path_Forbidden_Edges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Path_Required_Edges
DROP TABLE IF EXISTS `Path_Required_Edges`;
CREATE TABLE IF NOT EXISTS `Path_Required_Edges` (
  `Rchain` varchar(256) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(197) NOT NULL,
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Path_Required_Edges: ~19 rows (approximately)
DELETE FROM `Path_Required_Edges`;
/*!40000 ALTER TABLE `Path_Required_Edges` DISABLE KEYS */;
INSERT INTO `Path_Required_Edges` (`Rchain`, `child`, `parent`) VALUES
	('`a,b`', '`capability(prof0,student0)`', '`a`'),
	('`a,b`', '`diff(course0)`', '`grade(course0,student0)`'),
	('`a,b`', '`grade(course0,student0)`', '`b`'),
	('`a,b`', '`grade(course0,student0)`', '`intelligence(student0)`'),
	('`a,b`', '`intelligence(student0)`', '`a`'),
	('`a,b`', '`popularity(prof0)`', '`a`'),
	('`a,b`', '`popularity(prof0)`', '`teachingability(prof0)`'),
	('`a,b`', '`ranking(student0)`', '`intelligence(student0)`'),
	('`a,b`', '`salary(prof0,student0)`', '`a`'),
	('`a,b`', '`salary(prof0,student0)`', '`capability(prof0,student0)`'),
	('`a,b`', '`sat(course0,student0)`', '`b`'),
	('`a,b`', '`sat(course0,student0)`', '`grade(course0,student0)`'),
	('`a`', '`capability(prof0,student0)`', '`a`'),
	('`a`', '`popularity(prof0)`', '`teachingability(prof0)`'),
	('`a`', '`ranking(student0)`', '`intelligence(student0)`'),
	('`a`', '`salary(prof0,student0)`', '`a`'),
	('`b`', '`grade(course0,student0)`', '`b`'),
	('`b`', '`ranking(student0)`', '`intelligence(student0)`'),
	('`b`', '`sat(course0,student0)`', '`b`');
/*!40000 ALTER TABLE `Path_Required_Edges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.popularity(prof0)_CP
DROP TABLE IF EXISTS `popularity(prof0)_CP`;
CREATE TABLE IF NOT EXISTS `popularity(prof0)_CP` (
  `popularity(prof0)` varchar(45) DEFAULT NULL,
  `RA(prof0,student0)` varchar(5) DEFAULT NULL,
  `teachingability(prof0)` varchar(45) DEFAULT NULL,
  `ParentSum` bigint(20) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL,
  KEY `popularity(prof0)_CP` (`popularity(prof0)`,`RA(prof0,student0)`,`teachingability(prof0)`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.popularity(prof0)_CP: ~6 rows (approximately)
DELETE FROM `popularity(prof0)_CP`;
/*!40000 ALTER TABLE `popularity(prof0)_CP` DISABLE KEYS */;
INSERT INTO `popularity(prof0)_CP` (`popularity(prof0)`, `RA(prof0,student0)`, `teachingability(prof0)`, `ParentSum`, `local_mult`, `CP`, `likelihood`, `prior`) VALUES
	('1', 'F', '2', 1040, 72, 0.692308, -26.48, 0.333333),
	('1', 'T', '2', 100, 4, 0.400000, -3.67, 0.333333),
	('2', 'F', '2', 1040, 32, 0.307692, -37.72, 0.666667),
	('2', 'F', '3', 990, 99, 1.000000, 0.00, 0.666667),
	('2', 'T', '2', 100, 6, 0.600000, -3.06, 0.666667),
	('2', 'T', '3', 150, 15, 1.000000, 0.00, 0.666667);
/*!40000 ALTER TABLE `popularity(prof0)_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.PVariables
DROP TABLE IF EXISTS `PVariables`;
CREATE TABLE IF NOT EXISTS `PVariables` (
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `index_number` bigint(20) NOT NULL DEFAULT '0',
  `Tuples` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.PVariables: ~3 rows (approximately)
DELETE FROM `PVariables`;
/*!40000 ALTER TABLE `PVariables` DISABLE KEYS */;
INSERT INTO `PVariables` (`pvid`, `TABLE_NAME`, `index_number`, `Tuples`) VALUES
	('course0', 'course', 0, 10),
	('prof0', 'prof', 0, 6),
	('student0', 'student', 0, 38);
/*!40000 ALTER TABLE `PVariables` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.PVariables_From_List
DROP TABLE IF EXISTS `PVariables_From_List`;
CREATE TABLE IF NOT EXISTS `PVariables_From_List` (
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `Entries` varchar(133) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.PVariables_From_List: ~3 rows (approximately)
DELETE FROM `PVariables_From_List`;
/*!40000 ALTER TABLE `PVariables_From_List` DISABLE KEYS */;
INSERT INTO `PVariables_From_List` (`pvid`, `Entries`) VALUES
	('course0', 'course AS course0'),
	('prof0', 'prof AS prof0'),
	('student0', 'student AS student0');
/*!40000 ALTER TABLE `PVariables_From_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.PVariables_GroupBy_List
DROP TABLE IF EXISTS `PVariables_GroupBy_List`;
CREATE TABLE IF NOT EXISTS `PVariables_GroupBy_List` (
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `Entries` varchar(133) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.PVariables_GroupBy_List: ~6 rows (approximately)
DELETE FROM `PVariables_GroupBy_List`;
/*!40000 ALTER TABLE `PVariables_GroupBy_List` DISABLE KEYS */;
INSERT INTO `PVariables_GroupBy_List` (`pvid`, `Entries`) VALUES
	('course0', '`diff(course0)`'),
	('student0', '`intelligence(student0)`'),
	('prof0', '`popularity(prof0)`'),
	('student0', '`ranking(student0)`'),
	('course0', '`rating(course0)`'),
	('prof0', '`teachingability(prof0)`');
/*!40000 ALTER TABLE `PVariables_GroupBy_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.PVariables_Select_List
DROP TABLE IF EXISTS `PVariables_Select_List`;
CREATE TABLE IF NOT EXISTS `PVariables_Select_List` (
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `Entries` varchar(267) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.PVariables_Select_List: ~9 rows (approximately)
DELETE FROM `PVariables_Select_List`;
/*!40000 ALTER TABLE `PVariables_Select_List` DISABLE KEYS */;
INSERT INTO `PVariables_Select_List` (`pvid`, `Entries`) VALUES
	('course0', 'count(*) as "MULT"'),
	('prof0', 'count(*) as "MULT"'),
	('student0', 'count(*) as "MULT"'),
	('course0', 'course0.diff AS `diff(course0)`'),
	('student0', 'student0.intelligence AS `intelligence(student0)`'),
	('prof0', 'prof0.popularity AS `popularity(prof0)`'),
	('student0', 'student0.ranking AS `ranking(student0)`'),
	('course0', 'course0.rating AS `rating(course0)`'),
	('prof0', 'prof0.teachingability AS `teachingability(prof0)`');
/*!40000 ALTER TABLE `PVariables_Select_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Pvars_Family
DROP TABLE IF EXISTS `Pvars_Family`;
CREATE TABLE IF NOT EXISTS `Pvars_Family` (
  `child` varchar(197) NOT NULL DEFAULT '',
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Pvars_Family: ~22 rows (approximately)
DELETE FROM `Pvars_Family`;
/*!40000 ALTER TABLE `Pvars_Family` DISABLE KEYS */;
INSERT INTO `Pvars_Family` (`child`, `pvid`) VALUES
	('`capability(prof0,student0)`', 'prof0'),
	('`intelligence(student0)`', 'prof0'),
	('`popularity(prof0)`', 'prof0'),
	('`ranking(student0)`', 'prof0'),
	('`salary(prof0,student0)`', 'prof0'),
	('`grade(course0,student0)`', 'course0'),
	('`sat(course0,student0)`', 'course0'),
	('`capability(prof0,student0)`', 'student0'),
	('`intelligence(student0)`', 'student0'),
	('`popularity(prof0)`', 'student0'),
	('`ranking(student0)`', 'student0'),
	('`salary(prof0,student0)`', 'student0'),
	('`grade(course0,student0)`', 'student0'),
	('`sat(course0,student0)`', 'student0'),
	('`diff(course0)`', 'course0'),
	('`diff(course0)`', 'student0'),
	('`b`', 'student0'),
	('`b`', 'course0'),
	('`a`', 'prof0'),
	('`a`', 'student0'),
	('`rating(course0)`', 'course0'),
	('`teachingability(prof0)`', 'prof0');
/*!40000 ALTER TABLE `Pvars_Family` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Pvars_Not_In_Family
DROP TABLE IF EXISTS `Pvars_Not_In_Family`;
CREATE TABLE IF NOT EXISTS `Pvars_Not_In_Family` (
  `child` varchar(199) NOT NULL DEFAULT '',
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `Tuples` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Pvars_Not_In_Family: ~14 rows (approximately)
DELETE FROM `Pvars_Not_In_Family`;
/*!40000 ALTER TABLE `Pvars_Not_In_Family` DISABLE KEYS */;
INSERT INTO `Pvars_Not_In_Family` (`child`, `pvid`, `Tuples`) VALUES
	('`a`', 'course0', 10),
	('`b`', 'prof0', 6),
	('`capability(prof0,student0)`', 'course0', 10),
	('`diff(course0)`', 'prof0', 6),
	('`grade(course0,student0)`', 'prof0', 6),
	('`intelligence(student0)`', 'course0', 10),
	('`popularity(prof0)`', 'course0', 10),
	('`ranking(student0)`', 'course0', 10),
	('`rating(course0)`', 'prof0', 6),
	('`rating(course0)`', 'student0', 38),
	('`salary(prof0,student0)`', 'course0', 10),
	('`sat(course0,student0)`', 'prof0', 6),
	('`teachingability(prof0)`', 'course0', 10),
	('`teachingability(prof0)`', 'student0', 38);
/*!40000 ALTER TABLE `Pvars_Not_In_Family` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.ranking(student0)_CP
DROP TABLE IF EXISTS `ranking(student0)_CP`;
CREATE TABLE IF NOT EXISTS `ranking(student0)_CP` (
  `ranking(student0)` varchar(45) DEFAULT NULL,
  `RA(prof0,student0)` varchar(5) DEFAULT NULL,
  `intelligence(student0)` varchar(45) DEFAULT NULL,
  `ParentSum` bigint(20) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL,
  KEY `ranking(student0)_CP` (`ranking(student0)`,`RA(prof0,student0)`,`intelligence(student0)`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.ranking(student0)_CP: ~13 rows (approximately)
DELETE FROM `ranking(student0)_CP`;
/*!40000 ALTER TABLE `ranking(student0)_CP` DISABLE KEYS */;
INSERT INTO `ranking(student0)_CP` (`ranking(student0)`, `RA(prof0,student0)`, `intelligence(student0)`, `ParentSum`, `local_mult`, `CP`, `likelihood`, `prior`) VALUES
	('1', 'F', '3', 580, 42, 0.724138, -13.56, 0.210526),
	('1', 'T', '3', 80, 6, 0.750000, -1.73, 0.210526),
	('2', 'F', '2', 650, 25, 0.384615, -23.89, 0.210526),
	('2', 'F', '3', 580, 16, 0.275862, -20.61, 0.210526),
	('2', 'T', '2', 130, 5, 0.384615, -4.78, 0.210526),
	('2', 'T', '3', 80, 2, 0.250000, -2.77, 0.210526),
	('3', 'F', '2', 650, 35, 0.538462, -21.67, 0.184211),
	('3', 'T', '2', 130, 7, 0.538462, -4.33, 0.184211),
	('4', 'F', '1', 800, 32, 0.400000, -29.32, 0.184211),
	('4', 'F', '2', 650, 5, 0.076923, -12.82, 0.184211),
	('4', 'T', '1', 40, 4, 1.000000, 0.00, 0.184211),
	('4', 'T', '2', 130, 1, 0.076923, -2.56, 0.184211),
	('5', 'F', '1', 800, 48, 0.600000, -24.52, 0.210526);
/*!40000 ALTER TABLE `ranking(student0)_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.rating(course0)_CP
DROP TABLE IF EXISTS `rating(course0)_CP`;
CREATE TABLE IF NOT EXISTS `rating(course0)_CP` (
  `rating(course0)` varchar(200) NOT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.rating(course0)_CP: ~2 rows (approximately)
DELETE FROM `rating(course0)_CP`;
/*!40000 ALTER TABLE `rating(course0)_CP` DISABLE KEYS */;
INSERT INTO `rating(course0)_CP` (`rating(course0)`, `CP`, `local_mult`, `likelihood`, `prior`) VALUES
	('1', 0.300000, 3, -3.61, 0.300000),
	('2', 0.700000, 7, -2.50, 0.700000);
/*!40000 ALTER TABLE `rating(course0)_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RChain_pvars
DROP TABLE IF EXISTS `RChain_pvars`;
CREATE TABLE IF NOT EXISTS `RChain_pvars` (
  `rchain` varchar(20) NOT NULL DEFAULT '',
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RChain_pvars: ~7 rows (approximately)
DELETE FROM `RChain_pvars`;
/*!40000 ALTER TABLE `RChain_pvars` DISABLE KEYS */;
INSERT INTO `RChain_pvars` (`rchain`, `pvid`) VALUES
	('`a,b`', 'prof0'),
	('`a`', 'prof0'),
	('`a,b`', 'course0'),
	('`b`', 'course0'),
	('`a,b`', 'student0'),
	('`a`', 'student0'),
	('`b`', 'student0');
/*!40000 ALTER TABLE `RChain_pvars` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RelationsParents
DROP TABLE IF EXISTS `RelationsParents`;
CREATE TABLE IF NOT EXISTS `RelationsParents` (
  `ChildNode` varchar(197) NOT NULL DEFAULT '',
  `rnid` varchar(197) DEFAULT NULL,
  `2node` varchar(197) NOT NULL DEFAULT '',
  `NumAtts` bigint(21) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RelationsParents: ~10 rows (approximately)
DELETE FROM `RelationsParents`;
/*!40000 ALTER TABLE `RelationsParents` DISABLE KEYS */;
INSERT INTO `RelationsParents` (`ChildNode`, `rnid`, `2node`, `NumAtts`) VALUES
	('`salary(prof0,student0)`', '`a`', '`capability(prof0,student0)`', 5),
	('`diff(course0)`', '`b`', '`grade(course0,student0)`', 4),
	('`sat(course0,student0)`', '`b`', '`grade(course0,student0)`', 4),
	('`capability(prof0,student0)`', '`a`', '`a`', 1),
	('`grade(course0,student0)`', '`b`', '`b`', 1),
	('`intelligence(student0)`', '`a`', '`a`', 1),
	('`popularity(prof0)`', '`a`', '`a`', 1),
	('`ranking(student0)`', '`a`', '`a`', 1),
	('`salary(prof0,student0)`', '`a`', '`a`', 1),
	('`sat(course0,student0)`', '`b`', '`b`', 1);
/*!40000 ALTER TABLE `RelationsParents` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RelationsPars
DROP TABLE IF EXISTS `RelationsPars`;
CREATE TABLE IF NOT EXISTS `RelationsPars` (
  `ChildNode` varchar(197) NOT NULL DEFAULT '',
  `NumPars` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RelationsPars: ~8 rows (approximately)
DELETE FROM `RelationsPars`;
/*!40000 ALTER TABLE `RelationsPars` DISABLE KEYS */;
INSERT INTO `RelationsPars` (`ChildNode`, `NumPars`) VALUES
	('`capability(prof0,student0)`', 2),
	('`diff(course0)`', 4.999999999999999),
	('`grade(course0,student0)`', 2),
	('`intelligence(student0)`', 2),
	('`popularity(prof0)`', 2),
	('`ranking(student0)`', 2),
	('`salary(prof0,student0)`', 6),
	('`sat(course0,student0)`', 4.999999999999999);
/*!40000 ALTER TABLE `RelationsPars` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RelationTables
DROP TABLE IF EXISTS `RelationTables`;
CREATE TABLE IF NOT EXISTS `RelationTables` (
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `SelfRelationship` bigint(21) DEFAULT NULL,
  `Many_OneRelationship` bigint(21) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RelationTables: ~2 rows (approximately)
DELETE FROM `RelationTables`;
/*!40000 ALTER TABLE `RelationTables` DISABLE KEYS */;
INSERT INTO `RelationTables` (`TABLE_NAME`, `SelfRelationship`, `Many_OneRelationship`) VALUES
	('RA', 0, 0),
	('registration', 0, 0);
/*!40000 ALTER TABLE `RelationTables` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodeEdges
DROP TABLE IF EXISTS `RNodeEdges`;
CREATE TABLE IF NOT EXISTS `RNodeEdges` (
  `Rchain` varchar(256) NOT NULL,
  `child` varchar(197) NOT NULL,
  `parent` varchar(197) NOT NULL,
  PRIMARY KEY (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodeEdges: ~0 rows (approximately)
DELETE FROM `RNodeEdges`;
/*!40000 ALTER TABLE `RNodeEdges` DISABLE KEYS */;
/*!40000 ALTER TABLE `RNodeEdges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodes
DROP TABLE IF EXISTS `RNodes`;
CREATE TABLE IF NOT EXISTS `RNodes` (
  `orig_rnid` varchar(199) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `pvid1` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `pvid2` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `COLUMN_NAME1` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `COLUMN_NAME2` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `main` int(11) NOT NULL DEFAULT '0',
  `rnid` varchar(10) DEFAULT NULL,
  KEY `Index` (`pvid1`,`pvid2`,`TABLE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodes: ~2 rows (approximately)
DELETE FROM `RNodes`;
/*!40000 ALTER TABLE `RNodes` DISABLE KEYS */;
INSERT INTO `RNodes` (`orig_rnid`, `TABLE_NAME`, `pvid1`, `pvid2`, `COLUMN_NAME1`, `COLUMN_NAME2`, `main`, `rnid`) VALUES
	('`RA(prof0,student0)`', 'RA', 'prof0', 'student0', 'prof_id', 'student_id', 1, '`a`'),
	('`registration(course0,student0)`', 'registration', 'course0', 'student0', 'course_id', 'student_id', 1, '`b`');
/*!40000 ALTER TABLE `RNodes` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodes_1Nodes
DROP TABLE IF EXISTS `RNodes_1Nodes`;
CREATE TABLE IF NOT EXISTS `RNodes_1Nodes` (
  `rnid` varchar(10) DEFAULT NULL,
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `1nid` varchar(133) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `COLUMN_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodes_1Nodes: ~8 rows (approximately)
DELETE FROM `RNodes_1Nodes`;
/*!40000 ALTER TABLE `RNodes_1Nodes` DISABLE KEYS */;
INSERT INTO `RNodes_1Nodes` (`rnid`, `TABLE_NAME`, `1nid`, `COLUMN_NAME`, `pvid`) VALUES
	('`b`', 'registration', '`diff(course0)`', 'diff', 'course0'),
	('`a`', 'RA', '`popularity(prof0)`', 'popularity', 'prof0'),
	('`b`', 'registration', '`rating(course0)`', 'rating', 'course0'),
	('`a`', 'RA', '`teachingability(prof0)`', 'teachingability', 'prof0'),
	('`a`', 'RA', '`intelligence(student0)`', 'intelligence', 'student0'),
	('`b`', 'registration', '`intelligence(student0)`', 'intelligence', 'student0'),
	('`a`', 'RA', '`ranking(student0)`', 'ranking', 'student0'),
	('`b`', 'registration', '`ranking(student0)`', 'ranking', 'student0');
/*!40000 ALTER TABLE `RNodes_1Nodes` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodes_2Nodes
DROP TABLE IF EXISTS `RNodes_2Nodes`;
CREATE TABLE IF NOT EXISTS `RNodes_2Nodes` (
  `rnid` varchar(10) DEFAULT NULL,
  `2nid` varchar(199) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodes_2Nodes: ~4 rows (approximately)
DELETE FROM `RNodes_2Nodes`;
/*!40000 ALTER TABLE `RNodes_2Nodes` DISABLE KEYS */;
INSERT INTO `RNodes_2Nodes` (`rnid`, `2nid`) VALUES
	('`a`', '`capability(prof0,student0)`'),
	('`b`', '`grade(course0,student0)`'),
	('`a`', '`salary(prof0,student0)`'),
	('`b`', '`sat(course0,student0)`');
/*!40000 ALTER TABLE `RNodes_2Nodes` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodes_2Nodes_Family
DROP TABLE IF EXISTS `RNodes_2Nodes_Family`;
CREATE TABLE IF NOT EXISTS `RNodes_2Nodes_Family` (
  `ChildNode` varchar(197) NOT NULL,
  `Rnode` varchar(197) NOT NULL,
  `2Node` varchar(197) NOT NULL,
  `NumAtts` bigint(21) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodes_2Nodes_Family: ~2 rows (approximately)
DELETE FROM `RNodes_2Nodes_Family`;
/*!40000 ALTER TABLE `RNodes_2Nodes_Family` DISABLE KEYS */;
INSERT INTO `RNodes_2Nodes_Family` (`ChildNode`, `Rnode`, `2Node`, `NumAtts`) VALUES
	('`salary(prof0,student0)`', '`a`', '`capability(prof0,student0)`', 5),
	('`sat(course0,student0)`', '`b`', '`grade(course0,student0)`', 4);
/*!40000 ALTER TABLE `RNodes_2Nodes_Family` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodes_BN_Nodes
DROP TABLE IF EXISTS `RNodes_BN_Nodes`;
CREATE TABLE IF NOT EXISTS `RNodes_BN_Nodes` (
  `rnid` varchar(10) DEFAULT NULL,
  `Fid` varchar(199) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `main` int(11) DEFAULT NULL,
  KEY `Index_rnid` (`rnid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodes_BN_Nodes: ~12 rows (approximately)
DELETE FROM `RNodes_BN_Nodes`;
/*!40000 ALTER TABLE `RNodes_BN_Nodes` DISABLE KEYS */;
INSERT INTO `RNodes_BN_Nodes` (`rnid`, `Fid`, `main`) VALUES
	('`b`', '`diff(course0)`', 1),
	('`a`', '`intelligence(student0)`', 1),
	('`b`', '`intelligence(student0)`', 1),
	('`a`', '`popularity(prof0)`', 1),
	('`a`', '`ranking(student0)`', 1),
	('`b`', '`ranking(student0)`', 1),
	('`b`', '`rating(course0)`', 1),
	('`a`', '`teachingability(prof0)`', 1),
	('`a`', '`capability(prof0,student0)`', 1),
	('`b`', '`grade(course0,student0)`', 1),
	('`a`', '`salary(prof0,student0)`', 1),
	('`b`', '`sat(course0,student0)`', 1);
/*!40000 ALTER TABLE `RNodes_BN_Nodes` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodes_From_List
DROP TABLE IF EXISTS `RNodes_From_List`;
CREATE TABLE IF NOT EXISTS `RNodes_From_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(142) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodes_From_List: ~8 rows (approximately)
DELETE FROM `RNodes_From_List`;
/*!40000 ALTER TABLE `RNodes_From_List` DISABLE KEYS */;
INSERT INTO `RNodes_From_List` (`rnid`, `Entries`) VALUES
	('`a`', 'unielwin.prof AS prof0'),
	('`b`', 'unielwin.course AS course0'),
	('`a`', 'unielwin.student AS student0'),
	('`b`', 'unielwin.student AS student0'),
	('`a`', 'unielwin.RA AS `a`'),
	('`b`', 'unielwin.registration AS `b`'),
	('`a`', '(select "T" as `a`) as `temp_a`'),
	('`b`', '(select "T" as `b`) as `temp_b`');
/*!40000 ALTER TABLE `RNodes_From_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodes_GroupBy_List
DROP TABLE IF EXISTS `RNodes_GroupBy_List`;
CREATE TABLE IF NOT EXISTS `RNodes_GroupBy_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(199) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodes_GroupBy_List: ~14 rows (approximately)
DELETE FROM `RNodes_GroupBy_List`;
/*!40000 ALTER TABLE `RNodes_GroupBy_List` DISABLE KEYS */;
INSERT INTO `RNodes_GroupBy_List` (`rnid`, `Entries`) VALUES
	('`b`', '`diff(course0)`'),
	('`a`', '`popularity(prof0)`'),
	('`b`', '`rating(course0)`'),
	('`a`', '`teachingability(prof0)`'),
	('`a`', '`intelligence(student0)`'),
	('`b`', '`intelligence(student0)`'),
	('`a`', '`ranking(student0)`'),
	('`b`', '`ranking(student0)`'),
	('`a`', '`capability(prof0,student0)`'),
	('`b`', '`grade(course0,student0)`'),
	('`a`', '`salary(prof0,student0)`'),
	('`b`', '`sat(course0,student0)`'),
	('`a`', '`a`'),
	('`b`', '`b`');
/*!40000 ALTER TABLE `RNodes_GroupBy_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodes_inFamily
DROP TABLE IF EXISTS `RNodes_inFamily`;
CREATE TABLE IF NOT EXISTS `RNodes_inFamily` (
  `ChildNode` varchar(197) NOT NULL,
  `Rnode` varchar(197) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodes_inFamily: ~7 rows (approximately)
DELETE FROM `RNodes_inFamily`;
/*!40000 ALTER TABLE `RNodes_inFamily` DISABLE KEYS */;
INSERT INTO `RNodes_inFamily` (`ChildNode`, `Rnode`) VALUES
	('`capability(prof0,student0)`', '`a`'),
	('`grade(course0,student0)`', '`b`'),
	('`intelligence(student0)`', '`a`'),
	('`popularity(prof0)`', '`a`'),
	('`ranking(student0)`', '`a`'),
	('`salary(prof0,student0)`', '`a`'),
	('`sat(course0,student0)`', '`b`');
/*!40000 ALTER TABLE `RNodes_inFamily` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Rnodes_join_columnname_list
DROP TABLE IF EXISTS `Rnodes_join_columnname_list`;
CREATE TABLE IF NOT EXISTS `Rnodes_join_columnname_list` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(227) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Rnodes_join_columnname_list: ~4 rows (approximately)
DELETE FROM `Rnodes_join_columnname_list`;
/*!40000 ALTER TABLE `Rnodes_join_columnname_list` DISABLE KEYS */;
INSERT INTO `Rnodes_join_columnname_list` (`rnid`, `Entries`) VALUES
	('`a`', '`capability(prof0,student0)` varchar(5)  default  "N/A" '),
	('`a`', '`salary(prof0,student0)` varchar(5)  default  "N/A" '),
	('`b`', '`grade(course0,student0)` varchar(5)  default  "N/A" '),
	('`b`', '`sat(course0,student0)` varchar(5)  default  "N/A" ');
/*!40000 ALTER TABLE `Rnodes_join_columnname_list` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodes_pvars
DROP TABLE IF EXISTS `RNodes_pvars`;
CREATE TABLE IF NOT EXISTS `RNodes_pvars` (
  `rnid` varchar(10) DEFAULT NULL,
  `pvid` varchar(65) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `COLUMN_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `REFERENCED_COLUMN_NAME` varchar(64) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodes_pvars: ~4 rows (approximately)
DELETE FROM `RNodes_pvars`;
/*!40000 ALTER TABLE `RNodes_pvars` DISABLE KEYS */;
INSERT INTO `RNodes_pvars` (`rnid`, `pvid`, `TABLE_NAME`, `COLUMN_NAME`, `REFERENCED_COLUMN_NAME`) VALUES
	('`a`', 'prof0', 'prof', 'prof_id', 'prof_id'),
	('`b`', 'course0', 'course', 'course_id', 'course_id'),
	('`a`', 'student0', 'student', 'student_id', 'student_id'),
	('`b`', 'student0', 'student', 'student_id', 'student_id');
/*!40000 ALTER TABLE `RNodes_pvars` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodes_Select_List
DROP TABLE IF EXISTS `RNodes_Select_List`;
CREATE TABLE IF NOT EXISTS `RNodes_Select_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(278) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodes_Select_List: ~16 rows (approximately)
DELETE FROM `RNodes_Select_List`;
/*!40000 ALTER TABLE `RNodes_Select_List` DISABLE KEYS */;
INSERT INTO `RNodes_Select_List` (`rnid`, `Entries`) VALUES
	('`a`', 'count(*) as "MULT"'),
	('`b`', 'count(*) as "MULT"'),
	('`b`', 'course0.diff AS `diff(course0)`'),
	('`a`', 'prof0.popularity AS `popularity(prof0)`'),
	('`b`', 'course0.rating AS `rating(course0)`'),
	('`a`', 'prof0.teachingability AS `teachingability(prof0)`'),
	('`a`', 'student0.intelligence AS `intelligence(student0)`'),
	('`b`', 'student0.intelligence AS `intelligence(student0)`'),
	('`a`', 'student0.ranking AS `ranking(student0)`'),
	('`b`', 'student0.ranking AS `ranking(student0)`'),
	('`a`', '`a`.capability AS `capability(prof0,student0)`'),
	('`a`', '`a`.salary AS `salary(prof0,student0)`'),
	('`b`', '`b`.grade AS `grade(course0,student0)`'),
	('`b`', '`b`.sat AS `sat(course0,student0)`'),
	('`a`', '`a`'),
	('`b`', '`b`');
/*!40000 ALTER TABLE `RNodes_Select_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.RNodes_Where_List
DROP TABLE IF EXISTS `RNodes_Where_List`;
CREATE TABLE IF NOT EXISTS `RNodes_Where_List` (
  `rnid` varchar(10) DEFAULT NULL,
  `Entries` varchar(208) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.RNodes_Where_List: ~4 rows (approximately)
DELETE FROM `RNodes_Where_List`;
/*!40000 ALTER TABLE `RNodes_Where_List` DISABLE KEYS */;
INSERT INTO `RNodes_Where_List` (`rnid`, `Entries`) VALUES
	('`a`', '`a`.prof_id = prof0.prof_id'),
	('`b`', '`b`.course_id = course0.course_id'),
	('`a`', '`a`.student_id = student0.student_id'),
	('`b`', '`b`.student_id = student0.student_id');
/*!40000 ALTER TABLE `RNodes_Where_List` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.salary(prof0,student0)_CP
DROP TABLE IF EXISTS `salary(prof0,student0)_CP`;
CREATE TABLE IF NOT EXISTS `salary(prof0,student0)_CP` (
  `salary(prof0,student0)` varchar(45) DEFAULT NULL,
  `RA(prof0,student0)` varchar(5) DEFAULT NULL,
  `capability(prof0,student0)` varchar(45) DEFAULT NULL,
  `ParentSum` bigint(20) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL,
  KEY `salary(prof0,student0)_CP` (`salary(prof0,student0)`,`RA(prof0,student0)`,`capability(prof0,student0)`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.salary(prof0,student0)_CP: ~10 rows (approximately)
DELETE FROM `salary(prof0,student0)_CP`;
/*!40000 ALTER TABLE `salary(prof0,student0)_CP` DISABLE KEYS */;
INSERT INTO `salary(prof0,student0)_CP` (`salary(prof0,student0)`, `RA(prof0,student0)`, `capability(prof0,student0)`, `ParentSum`, `local_mult`, `CP`, `likelihood`, `prior`) VALUES
	('high', 'T', '3', 70, 2, 0.285714, -2.51, 0.048246),
	('high', 'T', '4', 50, 5, 1.000000, 0.00, 0.048246),
	('high', 'T', '5', 40, 4, 1.000000, 0.00, 0.048246),
	('low', 'T', '1', 50, 2, 0.400000, -1.83, 0.021930),
	('low', 'T', '2', 40, 2, 0.500000, -1.39, 0.021930),
	('low', 'T', '3', 70, 1, 0.142857, -1.95, 0.021930),
	('med', 'T', '1', 50, 3, 0.600000, -1.53, 0.039474),
	('med', 'T', '2', 40, 2, 0.500000, -1.39, 0.039474),
	('med', 'T', '3', 70, 4, 0.571429, -2.24, 0.039474),
	('N/A', 'F', 'N/A', 2030, 203, 1.000000, 0.00, 0.890351);
/*!40000 ALTER TABLE `salary(prof0,student0)_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.sat(course0,student0)_CP
DROP TABLE IF EXISTS `sat(course0,student0)_CP`;
CREATE TABLE IF NOT EXISTS `sat(course0,student0)_CP` (
  `sat(course0,student0)` varchar(45) DEFAULT NULL,
  `registration(course0,student0)` varchar(5) DEFAULT NULL,
  `grade(course0,student0)` varchar(45) DEFAULT NULL,
  `ParentSum` bigint(20) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL,
  KEY `sat(course0,student0)_CP` (`sat(course0,student0)`,`registration(course0,student0)`,`grade(course0,student0)`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.sat(course0,student0)_CP: ~9 rows (approximately)
DELETE FROM `sat(course0,student0)_CP`;
/*!40000 ALTER TABLE `sat(course0,student0)_CP` DISABLE KEYS */;
INSERT INTO `sat(course0,student0)_CP` (`sat(course0,student0)`, `registration(course0,student0)`, `grade(course0,student0)`, `ParentSum`, `local_mult`, `CP`, `likelihood`, `prior`) VALUES
	('1', 'T', '1', 192, 29, 0.906250, -2.85, 0.107895),
	('1', 'T', '2', 174, 12, 0.413793, -10.59, 0.107895),
	('2', 'T', '1', 192, 3, 0.093750, -7.10, 0.078947),
	('2', 'T', '2', 174, 15, 0.517241, -9.89, 0.078947),
	('2', 'T', '3', 108, 12, 0.666667, -4.87, 0.078947),
	('3', 'T', '2', 174, 2, 0.068966, -5.35, 0.055263),
	('3', 'T', '3', 108, 6, 0.333333, -6.59, 0.055263),
	('3', 'T', '4', 78, 13, 1.000000, 0.00, 0.055263),
	('N/A', 'F', 'N/A', 1728, 288, 1.000000, 0.00, 0.757895);
/*!40000 ALTER TABLE `sat(course0,student0)_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.SchemaEdges
DROP TABLE IF EXISTS `SchemaEdges`;
CREATE TABLE IF NOT EXISTS `SchemaEdges` (
  `Rchain` varchar(20) NOT NULL DEFAULT '',
  `child` varchar(199) CHARACTER SET utf8 NOT NULL DEFAULT '',
  `parent` varchar(10) DEFAULT NULL,
  KEY `HashIn` (`Rchain`,`child`,`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.SchemaEdges: ~8 rows (approximately)
DELETE FROM `SchemaEdges`;
/*!40000 ALTER TABLE `SchemaEdges` DISABLE KEYS */;
INSERT INTO `SchemaEdges` (`Rchain`, `child`, `parent`) VALUES
	('`a,b`', '`capability(prof0,student0)`', '`a`'),
	('`a,b`', '`grade(course0,student0)`', '`b`'),
	('`a,b`', '`salary(prof0,student0)`', '`a`'),
	('`a,b`', '`sat(course0,student0)`', '`b`'),
	('`a`', '`capability(prof0,student0)`', '`a`'),
	('`a`', '`salary(prof0,student0)`', '`a`'),
	('`b`', '`grade(course0,student0)`', '`b`'),
	('`b`', '`sat(course0,student0)`', '`b`');
/*!40000 ALTER TABLE `SchemaEdges` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.Scores
DROP TABLE IF EXISTS `Scores`;
CREATE TABLE IF NOT EXISTS `Scores` (
  `Fid` varchar(255) NOT NULL,
  `LogLikelihood` float(20,2) DEFAULT NULL,
  `Normal_LogLikelihood` float(20,2) DEFAULT NULL,
  `Parameters` bigint(20) DEFAULT NULL,
  `SampleSize` bigint(20) DEFAULT NULL,
  `BIC` float(20,2) DEFAULT NULL,
  `AIC` float(20,2) DEFAULT NULL,
  `Pseudo_BIC` float(20,2) DEFAULT NULL,
  `Pseudo_AIC` float(20,2) DEFAULT NULL,
  `Big_SampleSize` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`Fid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.Scores: ~12 rows (approximately)
DELETE FROM `Scores`;
/*!40000 ALTER TABLE `Scores` DISABLE KEYS */;
INSERT INTO `Scores` (`Fid`, `LogLikelihood`, `Normal_LogLikelihood`, `Parameters`, `SampleSize`, `BIC`, `AIC`, `Pseudo_BIC`, `Pseudo_AIC`, `Big_SampleSize`) VALUES
	('`a`', -78.28, -0.34, 2, 228, -167.42, -80.28, -11.54, -2.34, 2280),
	('`b`', -204.17, -0.54, 6, 380, -443.98, -210.17, -36.72, -6.54, 2280),
	('`capability(prof0,student0)`', -39.67, -0.17, 8, 228, -122.77, -47.67, -43.77, -8.17, 2280),
	('`diff(course0)`', -253.14, -0.67, 5, 380, -535.98, -258.14, -31.04, -5.67, 2280),
	('`grade(course0,student0)`', -73.22, -0.19, 18, 380, -253.36, -91.22, -107.30, -18.19, 2280),
	('`intelligence(student0)`', -246.12, -1.08, 4, 228, -513.96, -250.12, -23.88, -5.08, 2280),
	('`popularity(prof0)`', -70.93, -0.31, 4, 228, -163.58, -74.93, -22.34, -4.31, 2280),
	('`ranking(student0)`', -162.56, -0.71, 24, 228, -455.42, -186.56, -131.72, -24.71, 2280),
	('`rating(course0)`', -6.11, -0.61, 1, 10, -14.52, -7.11, -3.52, -1.61, 2280),
	('`salary(prof0,student0)`', -12.84, -0.06, 12, 228, -90.83, -24.84, -65.27, -12.06, 2280),
	('`sat(course0,student0)`', -47.24, -0.12, 10, 380, -153.88, -57.24, -59.64, -10.12, 2280),
	('`teachingability(prof0)`', -4.16, -0.69, 1, 6, -10.11, -5.16, -3.17, -1.69, 2280);
/*!40000 ALTER TABLE `Scores` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.TargetChildren
DROP TABLE IF EXISTS `TargetChildren`;
CREATE TABLE IF NOT EXISTS `TargetChildren` (
  `TargetNode` varchar(197) NOT NULL,
  `TargetChild` varchar(197) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.TargetChildren: ~16 rows (approximately)
DELETE FROM `TargetChildren`;
/*!40000 ALTER TABLE `TargetChildren` DISABLE KEYS */;
INSERT INTO `TargetChildren` (`TargetNode`, `TargetChild`) VALUES
	('`teachingability(prof0)`', '`RA(prof0,student0)`'),
	('`intelligence(student0)`', '`registration(course0,student0)`'),
	('`rating(course0)`', '`registration(course0,student0)`'),
	('`RA(prof0,student0)`', '`capability(prof0,student0)`'),
	('`grade(course0,student0)`', '`diff(course0)`'),
	('`registration(course0,student0)`', '`grade(course0,student0)`'),
	('`intelligence(student0)`', '`grade(course0,student0)`'),
	('`RA(prof0,student0)`', '`intelligence(student0)`'),
	('`RA(prof0,student0)`', '`popularity(prof0)`'),
	('`teachingability(prof0)`', '`popularity(prof0)`'),
	('`RA(prof0,student0)`', '`ranking(student0)`'),
	('`intelligence(student0)`', '`ranking(student0)`'),
	('`RA(prof0,student0)`', '`salary(prof0,student0)`'),
	('`capability(prof0,student0)`', '`salary(prof0,student0)`'),
	('`registration(course0,student0)`', '`sat(course0,student0)`'),
	('`grade(course0,student0)`', '`sat(course0,student0)`');
/*!40000 ALTER TABLE `TargetChildren` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.TargetChildrensParents
DROP TABLE IF EXISTS `TargetChildrensParents`;
CREATE TABLE IF NOT EXISTS `TargetChildrensParents` (
  `TargetNode` varchar(197) NOT NULL,
  `TargetChildParent` varchar(197) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.TargetChildrensParents: ~12 rows (approximately)
DELETE FROM `TargetChildrensParents`;
/*!40000 ALTER TABLE `TargetChildrensParents` DISABLE KEYS */;
INSERT INTO `TargetChildrensParents` (`TargetNode`, `TargetChildParent`) VALUES
	('`RA(prof0,student0)`', '`capability(prof0,student0)`'),
	('`RA(prof0,student0)`', '`intelligence(student0)`'),
	('`RA(prof0,student0)`', '`teachingability(prof0)`'),
	('`registration(course0,student0)`', '`grade(course0,student0)`'),
	('`registration(course0,student0)`', '`intelligence(student0)`'),
	('`capability(prof0,student0)`', '`RA(prof0,student0)`'),
	('`grade(course0,student0)`', '`registration(course0,student0)`'),
	('`intelligence(student0)`', '`RA(prof0,student0)`'),
	('`intelligence(student0)`', '`registration(course0,student0)`'),
	('`intelligence(student0)`', '`rating(course0)`'),
	('`rating(course0)`', '`intelligence(student0)`'),
	('`teachingability(prof0)`', '`RA(prof0,student0)`');
/*!40000 ALTER TABLE `TargetChildrensParents` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.TargetMB
DROP TABLE IF EXISTS `TargetMB`;
CREATE TABLE IF NOT EXISTS `TargetMB` (
  `TargetNode` varchar(197) NOT NULL DEFAULT '',
  `TargetMBNode` varchar(197) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.TargetMB: ~34 rows (approximately)
DELETE FROM `TargetMB`;
/*!40000 ALTER TABLE `TargetMB` DISABLE KEYS */;
INSERT INTO `TargetMB` (`TargetNode`, `TargetMBNode`) VALUES
	('`RA(prof0,student0)`', '`ranking(student0)`'),
	('`RA(prof0,student0)`', '`capability(prof0,student0)`'),
	('`RA(prof0,student0)`', '`salary(prof0,student0)`'),
	('`RA(prof0,student0)`', '`intelligence(student0)`'),
	('`RA(prof0,student0)`', '`teachingability(prof0)`'),
	('`RA(prof0,student0)`', '`popularity(prof0)`'),
	('`registration(course0,student0)`', '`intelligence(student0)`'),
	('`registration(course0,student0)`', '`grade(course0,student0)`'),
	('`registration(course0,student0)`', '`sat(course0,student0)`'),
	('`registration(course0,student0)`', '`rating(course0)`'),
	('`capability(prof0,student0)`', '`RA(prof0,student0)`'),
	('`capability(prof0,student0)`', '`salary(prof0,student0)`'),
	('`diff(course0)`', '`grade(course0,student0)`'),
	('`grade(course0,student0)`', '`registration(course0,student0)`'),
	('`grade(course0,student0)`', '`diff(course0)`'),
	('`grade(course0,student0)`', '`intelligence(student0)`'),
	('`grade(course0,student0)`', '`sat(course0,student0)`'),
	('`intelligence(student0)`', '`registration(course0,student0)`'),
	('`intelligence(student0)`', '`RA(prof0,student0)`'),
	('`intelligence(student0)`', '`rating(course0)`'),
	('`intelligence(student0)`', '`ranking(student0)`'),
	('`intelligence(student0)`', '`grade(course0,student0)`'),
	('`popularity(prof0)`', '`teachingability(prof0)`'),
	('`popularity(prof0)`', '`RA(prof0,student0)`'),
	('`ranking(student0)`', '`RA(prof0,student0)`'),
	('`ranking(student0)`', '`intelligence(student0)`'),
	('`rating(course0)`', '`intelligence(student0)`'),
	('`rating(course0)`', '`registration(course0,student0)`'),
	('`salary(prof0,student0)`', '`RA(prof0,student0)`'),
	('`salary(prof0,student0)`', '`capability(prof0,student0)`'),
	('`sat(course0,student0)`', '`registration(course0,student0)`'),
	('`sat(course0,student0)`', '`grade(course0,student0)`'),
	('`teachingability(prof0)`', '`RA(prof0,student0)`'),
	('`teachingability(prof0)`', '`popularity(prof0)`');
/*!40000 ALTER TABLE `TargetMB` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.TargetParents
DROP TABLE IF EXISTS `TargetParents`;
CREATE TABLE IF NOT EXISTS `TargetParents` (
  `TargetNode` varchar(197) NOT NULL,
  `TargetParent` varchar(197) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.TargetParents: ~16 rows (approximately)
DELETE FROM `TargetParents`;
/*!40000 ALTER TABLE `TargetParents` DISABLE KEYS */;
INSERT INTO `TargetParents` (`TargetNode`, `TargetParent`) VALUES
	('`RA(prof0,student0)`', '`teachingability(prof0)`'),
	('`registration(course0,student0)`', '`intelligence(student0)`'),
	('`registration(course0,student0)`', '`rating(course0)`'),
	('`capability(prof0,student0)`', '`RA(prof0,student0)`'),
	('`diff(course0)`', '`grade(course0,student0)`'),
	('`grade(course0,student0)`', '`registration(course0,student0)`'),
	('`grade(course0,student0)`', '`intelligence(student0)`'),
	('`intelligence(student0)`', '`RA(prof0,student0)`'),
	('`popularity(prof0)`', '`RA(prof0,student0)`'),
	('`popularity(prof0)`', '`teachingability(prof0)`'),
	('`ranking(student0)`', '`RA(prof0,student0)`'),
	('`ranking(student0)`', '`intelligence(student0)`'),
	('`salary(prof0,student0)`', '`RA(prof0,student0)`'),
	('`salary(prof0,student0)`', '`capability(prof0,student0)`'),
	('`sat(course0,student0)`', '`registration(course0,student0)`'),
	('`sat(course0,student0)`', '`grade(course0,student0)`');
/*!40000 ALTER TABLE `TargetParents` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.teachingability(prof0)_CP
DROP TABLE IF EXISTS `teachingability(prof0)_CP`;
CREATE TABLE IF NOT EXISTS `teachingability(prof0)_CP` (
  `teachingability(prof0)` varchar(200) NOT NULL,
  `CP` float(7,6) DEFAULT NULL,
  `local_mult` bigint(20) DEFAULT NULL,
  `likelihood` float(20,2) DEFAULT NULL,
  `prior` float(7,6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.teachingability(prof0)_CP: ~2 rows (approximately)
DELETE FROM `teachingability(prof0)_CP`;
/*!40000 ALTER TABLE `teachingability(prof0)_CP` DISABLE KEYS */;
INSERT INTO `teachingability(prof0)_CP` (`teachingability(prof0)`, `CP`, `local_mult`, `likelihood`, `prior`) VALUES
	('2', 0.500000, 3, -2.08, 0.500000),
	('3', 0.500000, 3, -2.08, 0.500000);
/*!40000 ALTER TABLE `teachingability(prof0)_CP` ENABLE KEYS */;


-- Dumping structure for table unielwin_BN.TernaryRelations
DROP TABLE IF EXISTS `TernaryRelations`;
CREATE TABLE IF NOT EXISTS `TernaryRelations` (
  `TABLE_NAME` varchar(64) CHARACTER SET utf8 NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table unielwin_BN.TernaryRelations: ~0 rows (approximately)
DELETE FROM `TernaryRelations`;
/*!40000 ALTER TABLE `TernaryRelations` DISABLE KEYS */;
/*!40000 ALTER TABLE `TernaryRelations` ENABLE KEYS */;


-- Dumping structure for view unielwin_BN.Entity_BN_nodes
DROP VIEW IF EXISTS `Entity_BN_nodes`;
-- Removing temporary table and create final VIEW structure
DROP TABLE IF EXISTS `Entity_BN_nodes`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER VIEW `Entity_BN_nodes` AS select `Entity_BayesNets`.`pvid` AS `pvid`,`Entity_BayesNets`.`child` AS `node` from `Entity_BayesNets` order by `Entity_BayesNets`.`pvid`;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
