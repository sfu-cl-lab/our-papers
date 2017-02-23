pdf(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumIMDB-bk.pdf') 
hs <- read.csv(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumIMDB.csv', head=TRUE, sep="," )

c<-rep(-0.9,226)
c2<-rep(0.5,226)
c3<-rep(-0.5,226)
c4<-rep(1,226)
c5<-rep(0,226)

plot(hs$ELD,c, pch=ifelse( hs$class <2, 0,8), col='black',main="Drama-Comedy Real dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3, xlim=c(0, 700000),ylim=c(-1,1),cex=0.8) 
text(200000,-1,"ELD", cex=1.2)


points(hs$FD*100,c2, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(200000,0.4,"FD", cex=1.2)


points(hs$absLR,c3, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(200000,-0.6,"|LR|", cex=1.2)

points(hs$LOG*2,c4, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(200000,0.9,"LOG", cex=1.2)


points(hs$LR/3,c5, pch=ifelse( hs$class <2, 0,8), col='black',ylab='',xaxt="n",cex=0.8) 
text(200000,-0.1,"LR", cex=1.2)

dev.off()

pdf(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumStriker-bk.pdf') 
hs <- read.csv(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumStriker.csv', head=TRUE, sep="," )
c<-rep(-0.9,175)
c2<-rep(0.5,175)
c3<-rep(-0.5,175)
c4<-rep(1,175)
c5<-rep(0,175)

plot(hs$ELD,c, pch=ifelse( hs$class <2, 0,8), col='black',main="Striker-Goalie Real dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3,xlim=c(0, 400),ylim=c(-1,1),cex=0.8) 
text(250,-1,"ELD", cex=1.2)


points(hs$FD*2,c2, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(250,0.4,"FD", cex=1.2)


points(hs$absLR,c3, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(250,-0.6,"|LR|", cex=1.2)

points(hs$LOG,c4, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(250,0.9,"LOG", cex=1.2)


points(hs$LR,c5, pch=ifelse( hs$class <2, 0,8), col='black',ylab='',xaxt="n",cex=0.8) 
text(250,-0.1,"LR", cex=1.2)

dev.off()

pdf(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumMidfielder-bk.pdf') 
hs <- read.csv(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumMidfielder.csv', head=TRUE, sep="," )
c<-rep(-0.9,229)
c2<-rep(0.5,229)
c3<-rep(-0.5,229)
c4<-rep(1,229)
c5<-rep(0,229)

plot(hs$ELD,c, pch=ifelse( hs$class <2, 0,8), col='black',main="Midfielder-Striker Real dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3,xlim=c(0, 350),ylim=c(-1,1),cex=0.8) 
text(200,-1,"ELD", cex=1.2)


points(hs$FD*2,c2, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(200,0.4,"FD", cex=1.2)


points(hs$absLR,c3,pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(200,-0.6,"|LR|", cex=1.2)

points(hs$LOG,c4, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(200,0.9,"LOG", cex=1.2)


points(hs$LR,c5, pch=ifelse( hs$class <2, 0,8), col='black',ylab='',xaxt="n",cex=0.8) 
text(200,-0.1,"LR", cex=1.2)

dev.off()





pdf(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumSep-bk.pdf') 
hs <- read.csv(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumSep.csv', head=TRUE, sep="," )
c<-rep(-0.9,280)
c2<-rep(0.5,280)
c3<-rep(-0.5,280)
c4<-rep(1,280)
c5<-rep(0,280)

plot(hs$ELD,c, pch=ifelse( hs$class <2, 0,8), col='black',main="Low-correlation Synthetic dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3,xlim=c(0, 70),ylim=c(-1,1),cex=0.8) 
text(30,-1,"ELD", cex=1.2)


points(hs$FD*4,c2, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(30,0.4,"FD", cex=1.2)


points(hs$absLR,c3, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(30,-0.6,"|LR|", cex=1.2)

points(hs$LOG,c4,pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(30,0.9,"LOG", cex=1.2)


points(hs$LR,c5, pch=ifelse( hs$class <2, 0,8), col='black',ylab='',xaxt="n",cex=0.8) 
text(30,-0.1,"LR", cex=1.2)

dev.off()


pdf(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumSV-bk.pdf') 
hs <- read.csv(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumSV.csv', head=TRUE, sep="," )
c<-rep(-0.9,280)
c2<-rep(0.5,280)
c3<-rep(-0.5,280)
c4<-rep(1,280)
c5<-rep(0,280)

plot(hs$ELD,c, pch=ifelse( hs$class <2, 0,8), col='black',main="High-correlation Synthetic dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3,xlim=c(0, 120),ylim=c(-1,1),cex=0.8) 
text(30,-1,"ELD", cex=1.2)


points(hs$FD*4,c2, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(30,0.4,"FD", cex=1.2)


points(hs$absLR,c3, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(30,-0.6,"|LR|", cex=1.2)

points(hs$LOG,c4, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(30,0.9,"LOG", cex=1.2)


points(hs$LR,c5, pch=ifelse( hs$class <2, 0,8), col='black',ylab='',xaxt="n",cex=0.8) 
text(30,-0.1,"LR", cex=1.2)

dev.off()


pdf(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumFeature-bk.pdf') 
hs <- read.csv(file='/local-scratch/SRiahi/iii/Thesis/Validation-Nov2015/Dec2-2015-1dPlots/sumFeature.csv', head=TRUE, sep="," )
c<-rep(-0.9,280)
c2<-rep(0.5,280)
c3<-rep(-0.5,280)
c4<-rep(1,280)
c5<-rep(0,280)

plot(hs$ELD-8,c, pch=ifelse( hs$class <2, 0,8), col='black',main="Single Feature Synthetic dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3,xlim=c(0, 60),ylim=c(-1,1),cex=0.8)  
text(30,-1,"ELD", cex=1.2)


points(hs$FD*4,c2,pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(30,0.4,"FD", cex=1.2)


points(hs$absLR,c3, pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(30,-0.6,"|LR|", cex=1.2)

points(hs$LOG,c4,pch=ifelse( hs$class <2, 0,8), col='black',xaxt="n",cex=0.8) 
text(30,0.9,"LOG", cex=1.2)


points(hs$LR,c5, pch=ifelse( hs$class <2, 0,8), col='black',ylab='',xaxt="n",cex=0.8) 
text(30,-0.1,"LR", cex=1.2)

dev.off()
