#PlayerID,Team1,Name,Salariescol,nationality,age,nationalitygroup,normalizedSalary,TeamStanding
pdf(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/PlayersNormalizedSalary.pdf') 
 boxplot(hs$normalizedSalary*1000/1000~hs$age, outline=FALSE, xlab='age',ylab='Normalized Salary')
dev.off()
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/WholeTable.csv', head=FALSE, sep="," )
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/Bernoulli-Sep-ELDvsFD.csv
', head=FALSE, sep="," )


 hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/WholeTable.csv', head=TRUE, sep="," )
> boxplot(hs$Salariescol/1000~hs$age)
> boxplot(hs$normalizedSalary/1000~hs$age)
> boxplot(hs$normalizedSalary/1000~hs$nationalitygroup)
> boxplot(hs$Salariescol/1000~hs$nationalitygroup)


pdf(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/TeamSalaries.pdf') 
 boxplot(hs$Salariescol~hs$Team1,outline=FALSE, xlab='Team',ylab='Salary of the players of the team')
 dev.off()

pdf(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/PlayersStatistics-Nov2015/TeamNormalSalaries.pdf') 
 boxplot(hs$normalizedSalary~hs$Team1,outline=FALSE, xlab='Team',ylab='Normalized Salary of the players of the team')
 dev.off()


