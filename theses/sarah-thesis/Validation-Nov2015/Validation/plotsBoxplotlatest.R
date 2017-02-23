#PlayerID,Team1,Name,Salariescol,nationality,age,nationalitygroup,normalizedSalary,TeamStanding

hs <- read.csv(file='/local-scratch/SRiahi/iii/iii/Thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/WholeTable.csv', head=FALSE, sep="," )

 hs <- read.csv(file='C:/Users/Fatemeh/Downloads/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/WholeTable.csv', head=TRUE, sep="," )
> boxplot(hs$Salariescol/1000~hs$age)
> boxplot(hs$normalizedSalary/1000~hs$age)
> boxplot(hs$normalizedSalary/1000~hs$nationalitygroup)
> boxplot(hs$Salariescol/1000~hs$nationalitygroup)

pdf(file='/local-scratch/SRiahi/iii/iii/Thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/TeamSal-2016.pdf') 
hs <- read.csv(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/WholeTable.csv', head=TRUE, sep="," )

# boxplot(hs$Salariescol~hs$Team1, xaxt='n',cex.axis=0.7,outline=FALSE)
# axis(1,at=seq(1,11,by=1), labels=c("Arsenal", "Aston Villa", "Chelsea", "Everton","Fullham", "Liverpool","Manchester City","Manchester United", "New Castle", "Sunderland", "Tottenham"),las=2,cex.axis=0.6)


 boxplot(hs$Salariescol~hs$Team1, xaxt='n',cex.axis=0.7,outline=FALSE)
 axis(1,at=seq(1,11,by=1), labels=c("Arsenal", "Aston Villa", "Chelsea", "Everton","Fulham", "Liverpool","Manchester City","Manchesterr United", "New Castle", "Sunderland", "Tottenham Hotspur"),las=2,cex.axis=0.6)
dev.off()


pdf(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/TeamNormalSal-2.pdf') 
hs <- read.csv(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/WholeTable.csv', head=TRUE, sep="," )

 boxplot(hs$normalizedSalary~hs$Team1, xaxt='n',cex.axis=0.7,outline=FALSE)
 axis(1,at=seq(1,11,by=1), labels=c("Arsenal", "Aston Villa", "Sunderland", "Everton","Manchester United", "Liverpool","Tottenham","Fullham", "New Castle", "Chelsea", "Manchester City"),las=2,cex.axis=0.6)

dev.off()

pdf(file='/local-scratch/SRiahi/iii/iii/Thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/nationalityNormalSalaries.pdf') 
hs <- read.csv(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/WholeTable.csv', head=TRUE, sep="," )

 boxplot(hs$Salariescol~hs$nationalitygroup, xaxt='n',cex.axis=0.7,outline=FALSE)
 axis(1,at=seq(1,7,by=1), labels=c("Africa", "France", "Ireland", "Other","South-America", "Spain","United Kingdom"),las=2,cex.axis=0.6)

dev.off()


pdf(file='/local-scratch/SRiahi/iii/iii/Thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/ageSalaries.pdf') 


 boxplot(hs$Salariescol~hs$age,cex.axis=0.7,outline=FALSE)
# axis(1,at=seq(1,7,by=1), labels=c("Africa", "France", "Ireland", "Other","South-America", "Spain","United Kingdom"),las=2,cex.axis=0.6)

dev.off()

> plot(hs$V2,c, pch=16, col=ifelse( hs$V1 <2, 'blue','red'), xlab='ELD',ylab='') 
>  points(hs$V3,c2, pch=16, col=ifelse( hs$V1 <2, 'blue','red'), xlab='ELD',ylab='',xaxt="n") 
> 
