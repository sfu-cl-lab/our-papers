
/******here are the views for original design************/

/*Required Edges: The required edges for the Bayes nets are 1) inherited from the entity tables for singleton relationship sets, 
and 2) inherited from the subsets for relationships of size > 1. This VIEW is updated every time Bayes net learning is performed.*/
CREATE OR REPLACE VIEW InheritedEdges AS
    SELECT DISTINCT 
        lattice_rel.child AS Rchain,
        Path_BayesNets.child AS child,
        Path_BayesNets.parent AS parent
    FROM
        Path_BayesNets, lattice_rel
    WHERE
        lattice_rel.parent = Path_BayesNets.Rchain
        AND Path_BayesNets.parent <> ''
    ORDER BY Rchain;
CREATE OR REPLACE VIEW Path_Required_Edges AS
    SELECT DISTINCT
        RNodes_pvars.rnid AS Rchain,
        Entity_BayesNets.child AS child,
        Entity_BayesNets.parent AS parent
    FROM
        RNodes_pvars, Entity_BayesNets
    WHERE
        RNodes_pvars.pvid = Entity_BayesNets.pvid 
        AND Entity_BayesNets.parent <> '' 
    UNION 
    SELECT DISTINCT Rchain, child, parent  FROM  InheritedEdges
    WHERE
        (Rchain , parent, child) NOT IN (SELECT * FROM InheritedEdges) 
    UNION 
    SELECT DISTINCT Rchain, parent, child FROM Knowledge_Required_Edges
    UNION  
    SELECT DISTINCT Rchain, parent, child FROM SchemaEdges;
CREATE OR REPLACE VIEW Entity_BN_nodes AS 
    SELECT 
        Entity_BayesNets.pvid AS pvid,
        Entity_BayesNets.child AS node
    FROM
        Entity_BayesNets 
    ORDER BY pvid;
CREATE OR REPLACE VIEW Entity_Complement_Edges AS
    SELECT distinct
        BN_nodes1.pvid AS pvid,
        BN_nodes1.node AS child,
        BN_nodes2.node AS parent
    FROM
        Entity_BN_nodes AS BN_nodes1, Entity_BN_nodes AS BN_nodes2
    WHERE
        BN_nodes1.pvid = BN_nodes2.pvid
        AND (NOT EXISTS (SELECT *  FROM  Entity_BayesNets
            WHERE   Entity_BayesNets.pvid = BN_nodes1.pvid
                AND Entity_BayesNets.child = BN_nodes1.node
                AND Entity_BayesNets.parent = BN_nodes2.node ) );
CREATE OR REPLACE VIEW Path_BN_nodes AS
    SELECT 
        lattice_membership.name AS Rchain, Fid AS node
    FROM
        lattice_membership, RNodes_BN_Nodes
    WHERE
        RNodes_BN_Nodes.rnid = lattice_membership.member
    ORDER BY lattice_membership.name; 
CREATE OR REPLACE VIEW Path_Complement_Edges AS
    SELECT 
        BN_nodes1.Rchain AS Rchain,
        BN_nodes1.node AS child,
        BN_nodes2.node AS parent
    FROM
        Path_BN_nodes AS BN_nodes1,Path_BN_nodes AS BN_nodes2
    WHERE
        BN_nodes1.Rchain = BN_nodes2.Rchain
        AND (NOT EXISTS (SELECT * FROM  Path_BayesNets 
            WHERE   Path_BayesNets.Rchain = BN_nodes1.Rchain   
                AND Path_BayesNets.child = BN_nodes1.node 
                AND Path_BayesNets.parent = BN_nodes2.node ) );

/****************************
Finally, the forbidden edges for the Bayes nets are 1) inherited from the entity tables for singleton relationship sets, 
and 2) inherited from the subsets for relationships of size > 1, and 3) the edges pointing into the main edges. 
Assumes that lattice_rel gives the subsets that are one size smaller.*/
CREATE OR REPLACE VIEW Path_Forbidden_Edges AS
    SELECT DISTINCT
        RNodes_pvars.rnid AS Rchain,
        Entity_Complement_Edges.child AS child,
        Entity_Complement_Edges.parent AS parent
    FROM
        RNodes_pvars, Entity_Complement_Edges
    WHERE
        RNodes_pvars.pvid = Entity_Complement_Edges.pvid 
    UNION 
    SELECT DISTINCT
        lattice_rel.child AS Rchain,
        Path_Complement_Edges.child AS child,
        Path_Complement_Edges.parent AS parent
    FROM
        Path_Complement_Edges, lattice_rel
    WHERE
        lattice_rel.parent = Path_Complement_Edges.Rchain
        AND Path_Complement_Edges.parent <> ''
        AND (lattice_rel.child, Path_Complement_Edges.child, Path_Complement_Edges.parent) 
            NOT in (select * from Path_Required_Edges) 
    UNION 
    SELECT Rchain , parent, child  FROM  Path_Aux_Edges 
    UNION 
    SELECT Rchain , parent, child  FROM  Knowledge_Forbidden_Edges;



/**----REAL queries for lattice-wise propagation used in the program--*/

-- initial
insert ignore into Path_Required_Edges 
    select  distinct 
        *    
    from        
        SchemaEdges;
insert ignore into Path_Forbidden_Edges   
    SELECT    distinct     
        *    
    FROM        
        Path_Aux_Edges ;

-- after Entity Table
insert ignore into Path_Required_Edges 
    select distinct  
        RNodes_pvars.rnid AS Rchain, Entity_BayesNets.child AS child,  Entity_BayesNets.parent AS parent  
    FROM  
        RNodes_pvars, Entity_BayesNets    
    WHERE 
        RNodes_pvars.pvid = Entity_BayesNets.pvid  AND Entity_BayesNets.parent <> '';
CREATE   table Entity_BN_Nodes AS   
    SELECT
        Entity_BayesNets.pvid AS pvid,   Entity_BayesNets.child AS node  
    FROM      
        Entity_BayesNets 
    ORDER BY pvid;
insert ignore into Entity_Complement_Edges  
    select distinct  
        BN_nodes1.pvid AS pvid,   BN_nodes1.node AS child, BN_nodes2.node AS parent  
    FROM   
        Entity_BN_Nodes AS BN_nodes1,   Entity_BN_Nodes AS BN_nodes2    
    WHERE   
        BN_nodes1.pvid = BN_nodes2.pvid   AND 
        (NOT EXISTS
            (SELECT  *  FROM  Entity_BayesNets  
                WHERE  
                    Entity_BayesNets.pvid = BN_nodes1.pvid  AND 
                    Entity_BayesNets.child = BN_nodes1.node  AND 
                    Entity_BayesNets.parent = BN_nodes2.node
            )
        );
insert ignore into Path_Forbidden_Edges 
    select distinct 
        RNodes_pvars.rnid AS Rchain, Entity_Complement_Edges.child AS child, Entity_Complement_Edges.parent AS parent 
    FROM 
        RNodes_pvars, Entity_Complement_Edges   
    WHERE  
        RNodes_pvars.pvid = Entity_Complement_Edges.pvid;

-- Rchain, from 1 to n
insert ignore into InheritedEdges 
    select distinct 
        lattice_rel.child AS Rchain, Path_BayesNets.child AS child,  Path_BayesNets.parent AS parent   
    FROM   
        Path_BayesNets,  lattice_rel,lattice_set    
    WHERE    
        lattice_rel.parent = Path_BayesNets.Rchain    AND  Path_BayesNets.parent <> '' and 
        lattice_set.name=lattice_rel.parent  and lattice_set.length = "len"    
    ORDER BY Rchain;

insert ignore into Path_Required_Edges 
    select distinct 
        Rchain, child, parent  
    from  
        InheritedEdges,lattice_set   
    where 
        Rchain = lattice_set.name and lattice_set.length ="len+1" and 
        (Rchain , parent, child) NOT IN (select * from   InheritedEdges)   and 
        child not in (select rnid from RNodes) ;

insert ignore into Path_Complement_Edges 
    select distinct  
        BN_nodes1.Rchain AS Rchain,  BN_nodes1.node AS child, BN_nodes2.node AS parent    
    FROM   
        Path_BN_nodes AS BN_nodes1,   Path_BN_nodes AS BN_nodes2,   lattice_set    
    WHERE 
        lattice_set.name=BN_nodes1.Rchain and lattice_set.length ="len" and 
        (   BN_nodes1.Rchain = BN_nodes2.Rchain  AND 
            (NOT EXISTS 
                (SELECT *   FROM  Path_BayesNets  
                    WHERE Path_BayesNets.Rchain = BN_nodes1.Rchain AND 
                          Path_BayesNets.child = BN_nodes1.node AND 
                          Path_BayesNets.parent = BN_nodes2.node
                )
            )
        ) ;

insert ignore into Path_Forbidden_Edges 
    select distinct  
        lattice_rel.child AS Rchain, Path_Complement_Edges.child AS child, Path_Complement_Edges.parent AS parent    
    FROM  
        Path_Complement_Edges,  lattice_rel, lattice_set    
    WHERE  
        lattice_set.name = lattice_rel.parent and lattice_set.length = "len" and  
        lattice_rel.parent = Path_Complement_Edges.Rchain  AND Path_Complement_Edges.parent <> ''   and 
        (lattice_rel.child, Path_Complement_Edges.child,  Path_Complement_Edges.parent) 
            not in (select  Rchain,child,parent from  Path_Required_Edges)  and 
         Path_Complement_Edges.parent 
            not in (select rnid from RNodes);
         





/*********************************************************/

--Metadata for building CT tables

CREATE TABLE ADT_PVariables_Select_List AS 
    SELECT DISTINCT
    pvid, CONCAT('count(*)',' AS "MULT"') AS Entries
    FROM
    PVariables
    UNION
    SELECT DISTINCT
    pvid,CONCAT(pvid, '.', COLUMN_NAME, ' AS ', 1nid) AS Entries 
    FROM
    1Nodes
        NATURAL JOIN
    PVariables;

CREATE TABLE ADT_PVariables_From_List AS 
    SELECT DISTINCT
    pvid, CONCAT('@database@.',TABLE_NAME, ' AS ', pvid) AS Entries 
    FROM
    PVariables;

CREATE TABLE ADT_PVariables_GroupBy_List AS
    SELECT DISTINCT  pvid, 1nid AS Entries 
    FROM    1Nodes   NATURAL JOIN  PVariables;


/** now to build tables for the relationship nodes **/

CREATE TABLE ADT_RNodes_1Nodes_Select_List AS 
    SELECT DISTINCT
    rnid, CONCAT('sum(`',REPLACE(rnid, '`', ''),'_counts`.`MULT`)',' AS "MULT"') AS Entries
    FROM    RNodes
    UNION
    SELECT DISTINCT rnid, 1nid AS Entries FROM RNodes_1Nodes;

CREATE TABLE ADT_RNodes_1Nodes_FROM_List AS 
    SELECT DISTINCT rnid, CONCAT('`',REPLACE(rnid, '`', ''),'_counts`') AS Entries 
    FROM RNodes;

CREATE TABLE ADT_RNodes_1Nodes_GroupBY_List AS 
    SELECT DISTINCT rnid, 1nid AS Entries 
    FROM  RNodes_1Nodes;

CREATE TABLE ADT_RNodes_Star_Select_List AS 
    SELECT DISTINCT rnid, 1nid AS Entries 
    FROM  RNodes_1Nodes;

CREATE TABLE ADT_RNodes_Star_From_List AS 
    SELECT DISTINCT rnid, CONCAT('`',REPLACE(pvid, '`', ''),'_counts`') AS Entries 
    FROM    RNodes_pvars;


CREATE TABLE ADT_RNodes_False_Select_List AS
    SELECT DISTINCT rnid, CONCAT('(`',REPLACE(rnid, '`', ''),'_star`.MULT','-','`',REPLACE(rnid, '`', ''),'_flat`.MULT)',' AS "MULT"') AS Entries
    FROM RNodes
    UNION
    SELECT DISTINCT rnid, CONCAT('`',REPLACE(rnid, '`', ''),'_star`.',1nid) AS Entries 
    FROM RNodes_1Nodes;

CREATE TABLE ADT_RNodes_False_FROM_List AS
    SELECT DISTINCT rnid, CONCAT('`',REPLACE(rnid, '`', ''),'_star`') AS Entries from RNodes
    UNION
    SELECT DISTINCT rnid, CONCAT('`',REPLACE(rnid, '`', ''),'_flat`') AS Entries from RNodes;

CREATE TABLE ADT_RNodes_False_WHERE_List AS
    SELECT DISTINCT rnid, CONCAT('`',REPLACE(rnid, '`', ''),'_star`.',1nid,'=','`',REPLACE(rnid, '`', ''),'_flat`.',1nid) AS Entries 
    FROM RNodes_1Nodes; 

/* May 16th, last step for _CT tables, preparing the colunmname_list */
CREATE table Rnodes_join_columnname_list AS 
    select distinct rnid,CONCAT(2nid, ' varchar(5)  default ',' "N/A" ') AS Entries 
    from 2Nodes natural join RNodes;

CREATE TABLE RChain_pvars AS
    select  distinct 
    lattice_membership.name AS rchain, 
    pvid 
    from 
    lattice_membership, RNodes_pvars 
    WHERE 
    RNodes_pvars.rnid = lattice_membership.member;

CREATE TABLE ADT_RChain_Star_From_List AS 
    SELECT DISTINCT 
    lattice_rel.child AS rchain, 
    lattice_rel.removed AS rnid, 
    CONCAT('`',REPLACE(lattice_rel.parent,'`',''),'_CT`')  AS Entries 
    FROM
    lattice_rel
    WHERE 
    lattice_rel.parent <>'EmptySet'
    UNION
    SELECT DISTINCT 
    lattice_rel.child AS rchain, 
    lattice_rel.removed AS rnid, 
    CONCAT('`',REPLACE(RNodes_pvars.pvid, '`', ''),'_counts`')    AS Entries 
    FROM
    lattice_rel,RNodes_pvars
    WHERE lattice_rel.parent <>'EmptySet'
    and RNodes_pvars.rnid = lattice_rel.removed and
    RNodes_pvars.pvid NOT in (select pvid from RChain_pvars WHERE RChain_pvars.rchain = lattice_rel.parent);

CREATE TABLE ADT_RChain_Star_Where_List AS 
    SELECT DISTINCT 
    lattice_rel.child AS rchain, 
    lattice_rel.removed AS rnid, 
    CONCAT(lattice_membership.member,' = "T"')  AS Entries 
    FROM
    lattice_rel,    lattice_membership
    WHERE  
    lattice_rel.child = lattice_membership.name
    and  lattice_membership.member > lattice_rel.removed
    and lattice_rel.parent <>'EmptySet';

CREATE TABLE ADT_RChain_Star_Select_List AS 
    SELECT DISTINCT 
    lattice_rel.child AS rchain, 
    lattice_rel.removed AS rnid, 
    RNodes_GroupBy_List.Entries 
    FROM
    lattice_rel,lattice_membership,RNodes_GroupBy_List
    WHERE 
    lattice_rel.parent <>'EmptySet'  AND lattice_membership.name = lattice_rel.parent
    AND RNodes_GroupBy_List.rnid = lattice_membership.member
    UNION
    SELECT DISTINCT 
    lattice_rel.child AS rchain, 
    lattice_rel.removed AS rnid, 
    1Nodes.1nid    AS Entries 
    FROM
    lattice_rel,RNodes_pvars,1Nodes
    WHERE 
    lattice_rel.parent <>'EmptySet' AND RNodes_pvars.rnid = lattice_rel.removed 
    AND RNodes_pvars.pvid = 1Nodes.pvid 
    AND 1Nodes.pvid NOT IN (SELECT pvid FROM RChain_pvars WHERE RChain_pvars.rchain =  lattice_rel.parent)
    UNION
    SELECT DISTINCT 
    lattice_rel.removed AS rchain, 
    lattice_rel.removed AS rnid, 
    1Nodes.1nid    AS Entries 
    FROM
    lattice_rel,RNodes_pvars,1Nodes
    WHERE 
    lattice_rel.parent ='EmptySet' AND RNodes_pvars.rnid = lattice_rel.removed 
    AND RNodes_pvars.pvid = 1Nodes.pvid;



/*                        ############################################
##################################################################################### */

-- Jan 13rd, real SQL queries for generating a_CT table
create table prof0_counts as 
    Select 
        count(*) as "MULT" ,                -- select string 
        prof0.popularity AS `popularity(prof0)` , prof0.teachingability AS `teachingability(prof0)` 
    from 
        unielwin.prof AS prof0              -- from string
    group by
        `popularity(prof0)` , `teachingability(prof0)` -- group by string
create table student0_counts as 
    Select 
        count(*) as "MULT" , 
        student0.intelligence AS `intelligence(student0)` , 
        student0.ranking AS `ranking(student0)` 
    from 
        unielwin.student AS student0 
    group by
        `intelligence(student0)` , `ranking(student0)`
CREATE TABLE ADT_PVariables_Select_List AS 
    SELECT DISTINCT
        pvid, CONCAT('count(*)',' AS "MULT"') AS Entries
    FROM
        PVariables
    UNION
    SELECT DISTINCT
        pvid,CONCAT(pvid, '.', COLUMN_NAME, ' AS ', 1nid) AS Entries 
    FROM
        1Nodes  NATURAL JOIN  PVariables;
CREATE TABLE ADT_PVariables_From_List AS 
    SELECT DISTINCT
        pvid, CONCAT('@database@.',TABLE_NAME, ' AS ', pvid) AS Entries 
    FROM
        PVariables;
CREATE TABLE ADT_PVariables_GroupBy_List AS
    SELECT DISTINCT  
        pvid, 1nid AS Entries 
    FROM    
        1Nodes   NATURAL JOIN  PVariables;
-- CT*
create table `a_star` as 
    Select 
        `prof0_counts`.`MULT`  * `student0_counts`.`MULT`  as `MULT` , -- mult string 
        `popularity(prof0)` , `teachingability(prof0)` ,  -- select string 
        `intelligence(student0)` , `ranking(student0)` 
    from 
    `prof0_counts` , `student0_counts`   -- from string
CREATE TABLE ADT_RNodes_Star_Select_List AS 
    SELECT DISTINCT 
        rnid, 1nid AS Entries 
    FROM  
        RNodes_1Nodes;
CREATE TABLE ADT_RNodes_Star_From_List AS 
    SELECT DISTINCT 
        rnid, CONCAT('`',REPLACE(pvid, '`', ''),'_counts`') AS Entries 
    FROM    
        RNodes_pvars;
-- mult string 
--concat the result set of  ADT_RNodes_Star_From_List with .mult

-- CTT
create table `a_counts` as 
    Select 
        count(*) as "MULT" ,   -- select string 
        prof0.popularity AS `popularity(prof0)` ,   prof0.teachingability AS `teachingability(prof0)` ,
        student0.intelligence AS `intelligence(student0)` , student0.ranking AS `ranking(student0)` , 
        `a`.capability AS `capability(prof0,student0)` ,  `a`.salary AS `salary(prof0,student0)` , `a` 
    from 
        unielwin.prof AS prof0 ,    unielwin.student AS student0 , unielwin.RA AS `a` , (select "T" as `a`) as `temp_a`  -- from string
    where 
        `a`.prof_id = prof0.prof_id and `a`.student_id =student0.student_id   -- where string
    group by
    `popularity(prof0)` , `teachingability(prof0)` ,    -- group by string
    `intelligence(student0)` , `ranking(student0)` , 
    `capability(prof0,student0)`, `salary(prof0,student0)` , 
    `a`
CREATE TABLE RNodes_Select_List AS 
    select 
        rnid, concat('count(*)',' as "MULT"') AS Entries  from    RNodes
    union
        SELECT DISTINCT rnid,     CONCAT(pvid, '.', COLUMN_NAME, ' AS ', 1nid) AS Entries  FROM    RNodes_1Nodes 
    UNION DISTINCT 
        select temp.rnid,temp.Entries from (SELECT DISTINCT  rnid,  CONCAT(rnid, '.', COLUMN_NAME, ' AS ', 2nid) AS Entries  
    FROM    2Nodes      NATURAL JOIN    RNodes order by RNodes.rnid,COLUMN_NAME) as temp
    UNION distinct 
        select     rnid, rnid AS Entries from   RNodes;
CREATE TABLE RNodes_From_List AS 
    SELECT DISTINCT rnid, CONCAT('@database@.',TABLE_NAME, ' AS ', pvid) AS Entries 
    FROM
        RNodes_pvars 
    UNION DISTINCT 
        SELECT DISTINCT   rnid, CONCAT('@database@.',TABLE_NAME, ' AS ', rnid) AS Entries
    FROM    RNodes 
    union distinct 
    select distinct
        rnid, concat('(select "T" as ', rnid,') as ', concat('`temp_', replace(rnid, '`', ''), '`')) as Entries
    from
        RNodes;
CREATE TABLE RNodes_Where_List AS 
    SELECT 
        rnid, CONCAT(rnid,'.',COLUMN_NAME,' = ',pvid,'.', REFERENCED_COLUMN_NAME) AS Entries 
    FROM
        RNodes_pvars;
CREATE TABLE RNodes_GroupBy_List AS 
    SELECT DISTINCT 
        rnid, 1nid AS Entries 
    FROM
        RNodes_1Nodes 
    UNION DISTINCT 
    SELECT DISTINCT
        rnid, 2nid AS Entries
    FROM
        2Nodes NATURAL JOIN   RNodes 
    UNION distinct 
    select 
        rnid, rnid
    from
        RNodes;

-- temp
create table `a_flat` as 
    Select 
        sum(`a_counts`.`MULT`) as "MULT" ,   -- select string 
        `popularity(prof0)` , `teachingability(prof0)` , 
        `intelligence(student0)` , `ranking(student0)` 
    from 
        `a_counts`   -- from string 
    group by  
        `popularity(prof0)` , `teachingability(prof0)` ,    -- group by string
        `intelligence(student0)` , `ranking(student0)`
CREATE TABLE ADT_RNodes_1Nodes_Select_List AS 
    SELECT DISTINCT
        rnid, CONCAT('sum(`',REPLACE(rnid, '`', ''),'_counts`.`MULT`)',' AS "MULT"') AS Entries
    FROM    
        RNodes
    UNION
    SELECT DISTINCT 
        rnid, 1nid AS Entries 
    FROM 
        RNodes_1Nodes;
CREATE TABLE ADT_RNodes_1Nodes_FROM_List AS 
    SELECT DISTINCT 
        rnid, CONCAT('`',REPLACE(rnid, '`', ''),'_counts`') AS Entries 
    FROM 
        RNodes;
CREATE TABLE ADT_RNodes_1Nodes_GroupBY_List AS 
    SELECT DISTINCT 
        rnid, 1nid AS Entries 
    FROM  
        RNodes_1Nodes;

-- CTF
CREATE TABLE CTF AS 
    SELECT 
        (CT*.count - temp.count) AS count,    -- select string 1
        CL(temp) 
    FROM 
        CT* , temp                              -- from string 1
    WHERE  
        CL(CT*) = CL (temp)                     -- where string 1
    union 
    SELECT 
        (CT*.COUNT) AS COUNT, 
        CL(CT*)                             -- select string 2 same as ADT_RNodes_Star_Select_List
    FROM  
        CT*                                 -- from string 2 same as ADT_RNodes_Star_From_List
    WHERE 
        CL(CT*)  NOT IN  
        (SELECT CL(temp) FROM temp )
create table ADT_RNodes_False_Select_List as
    SELECT DISTINCT 
        rnid, concat('(`',replace(rnid, '`', ''),'_star`.MULT','-','`',replace(rnid, '`', ''),'_flat`.MULT)',' AS "MULT"') as Entries
    from 
        RNodes
    union
    SELECT DISTINCT 
        rnid, concat('`',replace(rnid, '`', ''),'_star`.',1nid) AS Entries 
    FROM
        RNodes_1Nodes;
CREATE TABLE ADT_RNodes_False_FROM_List AS
    SELECT DISTINCT rnid, CONCAT('`',REPLACE(rnid, '`', ''),'_star`') AS Entries 
    from 
        RNodes
    UNION
    SELECT DISTINCT 
        rnid, CONCAT('`',REPLACE(rnid, '`', ''),'_flat`') AS Entries 
    from 
        RNodes;
CREATE TABLE ADT_RNodes_False_WHERE_List AS
    SELECT DISTINCT 
        rnid, CONCAT('`',REPLACE(rnid, '`', ''),'_star`.',1nid,'=','`',REPLACE(rnid, '`', ''),'_flat`.',1nid) AS Entries 
    FROM 
        RNodes_1Nodes; 
-- CT
create table `a_CT` as 
    select 
        `MULT` , 
        `popularity(prof0)` , `teachingability(prof0)` , 
        `intelligence(student0)` , `ranking(student0)` , 
        `capability(prof0,student0)` , `salary(prof0,student0)` , 
        `a` 
    from 
        `a_counts` 
    union 
    select 
        `MULT` , 
        `popularity(prof0)` , `teachingability(prof0)` , 
        `intelligence(student0)` , `ranking(student0)` , 
        `capability(prof0,student0)` , `salary(prof0,student0)` , 
        `a` 
    from `a_false`, `a_join`;
CREATE TABLE RNodes_1Nodes AS 
    SELECT 
        rnid, TABLE_NAME, 1nid, COLUMN_NAME, pvid1 AS pvid 
    FROM    
        RNodes, 1Nodes
    WHERE
        1Nodes.pvid = RNodes.pvid1 
    UNION 
    SELECT 
        rnid, TABLE_NAME, 1nid, COLUMN_NAME, pvid2 AS pvid
    FROM
        RNodes, 1Nodes
    WHERE
        1Nodes.pvid = RNodes.pvid2;










CREATE VIEW InheritedEdges AS
    SELECT DISTINCT 
        lattice_rel.child AS Rchain,
        Path_BayesNets.child AS child,
        Path_BayesNets.parent AS parent
    FROM
        Path_BayesNets, lattice_rel
    WHERE
        lattice_rel.parent = Path_BayesNets.Rchain
        AND Path_BayesNets.parent <> '';