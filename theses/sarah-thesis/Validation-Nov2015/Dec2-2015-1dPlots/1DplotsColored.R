pdf(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumIMDB.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumIMDB.csv', head=TRUE, sep="," )

c<-rep(0,226)
c2<-rep(1,226
c3<-rep(0.5,226)
c4<-rep(-0.5,226)
c5<-rep(-0.9,226)

plot(hs$ELD,c, pch=16, col=ifelse(  hs$class <2, 'blue','red'),main="Drama-Comedy Real dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3) 
text(200000,-0.1,"ELD distribution", cex=1.2)


points(hs$FD*100,c2, pch=16, col=ifelse( hs$class <2, 'blue','red'),xaxt="n") 
text(200000,0.9,"FD distribution", cex=1.2)


points(hs$absLR,c3, pch=16, col=ifelse( hs$class <2, 'blue','red'),xaxt="n") 
text(200000,0.4,"|LR| distribution", cex=1.2)

points(hs$LOG*2,c4, pch=16, col=ifelse( hs$class <2, 'blue','red'),xaxt="n") 
text(200000,-0.6,"LOG distribution", cex=1.2)


points(hs$LR/3,c5, pch=16, col=ifelse( hs$class <2, 'blue','red'),ylab='',xaxt="n") 
text(200000,-1,"LR distribution", cex=1.2)

dev.off()

pdf(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumStriker.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumStriker.csv', head=TRUE, sep="," )
c<-rep(0,175)
c2<-rep(1,175)
c3<-rep(0.5,175)
c4<-rep(-0.5,175)
c5<-rep(-0.9,175)

plot(hs$ELD,c, pch=16, col=ifelse(  hs$class >2, 'blue','red'),main="Striker-Goalie Real dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3) 
text(250,-0.1,"ELD distribution", cex=1.2)


points(hs$FD*2,c2, pch=16, col=ifelse( hs$class >2, 'blue','red'),xaxt="n") 
text(250,0.9,"FD distribution", cex=1.2)


points(hs$absLR,c3, pch=16, col=ifelse( hs$class >2, 'blue','red'),xaxt="n") 
text(250,0.4,"|LR| distribution", cex=1.2)

points(hs$LOG,c4, pch=16, col=ifelse( hs$class >2, 'blue','red'),xaxt="n") 
text(250,-0.6,"LOG distribution", cex=1.2)


points(hs$LR,c5, pch=16, col=ifelse( hs$class >2, 'blue','red'),ylab='',xaxt="n") 
text(250,-1,"LR distribution", cex=1.2)

dev.off()

pdf(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumMidfielder.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumMidfielder.csv', head=TRUE, sep="," )
c<-rep(0,229)
c2<-rep(1,229)
c3<-rep(0.5,229)
c4<-rep(-0.5,229)
c5<-rep(-0.9,229)

plot(hs$ELD,c, pch=16, col=ifelse(  hs$class<5, 'blue','red'),main="Midfielder-Striker Real dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3) 
text(200,-0.1,"ELD distribution", cex=1.2)


points(hs$FD*2,c2, pch=16, col=ifelse( hs$class <5, 'blue','red'),xaxt="n") 
text(200,0.9,"FD distribution", cex=1.2)


points(hs$absLR,c3, pch=16, col=ifelse( hs$class <5, 'blue','red'),xaxt="n") 
text(200,0.4,"|LR| distribution", cex=1.2)

points(hs$LOG,c4, pch=16, col=ifelse( hs$class <5, 'blue','red'),xaxt="n") 
text(200,-0.6,"LOG distribution", cex=1.2)


points(hs$LR,c5, pch=16, col=ifelse( hs$class <5, 'blue','red'),ylab='',xaxt="n") 
text(200,-1,"LR distribution", cex=1.2)

dev.off()


pdf(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumMidfielder.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumMidfielder.csv', head=TRUE, sep="," )
c<-rep(0,229)
c2<-rep(1,229)
c3<-rep(0.5,229)
c4<-rep(-0.5,229)
c5<-rep(-0.9,229)

plot(hs$ELD,c, pch=16, col=ifelse(  hs$class<5, 'blue','red'),main="Midfielder-Striker Real dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3) 
text(200,-0.1,"ELD distribution", cex=1.2)


points(hs$FD*2,c2, pch=16, col=ifelse( hs$class <5, 'blue','red'),xaxt="n") 
text(200,0.9,"FD distribution", cex=1.2)


points(hs$absLR,c3, pch=16, col=ifelse( hs$class <5, 'blue','red'),xaxt="n") 
text(200,0.4,"|LR| distribution", cex=1.2)

points(hs$LOG,c4, pch=16, col=ifelse( hs$class <5, 'blue','red'),xaxt="n") 
text(200,-0.6,"LOG distribution", cex=1.2)


points(hs$LR,c5, pch=16, col=ifelse( hs$class <5, 'blue','red'),ylab='',xaxt="n") 
text(200,-1,"LR distribution", cex=1.2)

dev.off()


pdf(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumMidfielder.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumMidfielder.csv', head=TRUE, sep="," )
c<-rep(0,229)
c2<-rep(1,229)
c3<-rep(0.5,229)
c4<-rep(-0.5,229)
c5<-rep(-0.9,229)

plot(hs$ELD,c, pch=16, col=ifelse(  hs$class<5, 'blue','red'),main="Midfielder-Striker Real dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3) 
text(200,-0.1,"ELD distribution", cex=1.2)


points(hs$FD*2,c2, pch=16, col=ifelse( hs$class <5, 'blue','red'),xaxt="n") 
text(200,0.9,"FD distribution", cex=1.2)


points(hs$absLR,c3, pch=16, col=ifelse( hs$class <5, 'blue','red'),xaxt="n") 
text(200,0.4,"|LR| distribution", cex=1.2)

points(hs$LOG,c4, pch=16, col=ifelse( hs$class <5, 'blue','red'),xaxt="n") 
text(200,-0.6,"LOG distribution", cex=1.2)


points(hs$LR,c5, pch=16, col=ifelse( hs$class <5, 'blue','red'),ylab='',xaxt="n") 
text(200,-1,"LR distribution", cex=1.2)

dev.off()


pdf(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumSep.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumSep.csv', head=TRUE, sep="," )
c<-rep(0,280)
c2<-rep(1,280)
c3<-rep(0.5,280)
c4<-rep(-0.5,280)
c5<-rep(-0.9,280)

plot(hs$ELD,c, pch=16, col=ifelse(  hs$class<2, 'blue','red'),main="Low-correlation Synthetic dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3) 
text(30,-0.1,"ELD distribution", cex=1.2)


points(hs$FD*4,c2, pch=16, col=ifelse( hs$classfd <2, 'blue','red'),xaxt="n") 
text(30,0.9,"FD distribution", cex=1.2)


points(hs$absLR,c3, pch=16, col=ifelse( hs$classabs <2, 'blue','red'),xaxt="n") 
text(30,0.4,"|LR| distribution", cex=1.2)

points(hs$LOG,c4, pch=16, col=ifelse( hs$classlg >1, 'blue','red'),xaxt="n") 
text(30,-0.6,"LOG distribution", cex=1.2)


points(hs$LR,c5, pch=16, col=ifelse( hs$classlr <2, 'blue','red'),ylab='',xaxt="n") 
text(30,-1,"LR distribution", cex=1.2)

dev.off()


pdf(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumSV.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumSV.csv', head=TRUE, sep="," )
c<-rep(0,280)
c2<-rep(1,280)
c3<-rep(0.5,280)
c4<-rep(-0.5,280)
c5<-rep(-0.9,280)

plot(hs$ELD,c, pch=16, col=ifelse(  hs$class<2, 'blue','red'),main="High-correlation Synthetic dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3) 
text(30,-0.1,"ELD distribution", cex=1.2)


points(hs$FD*4,c2, pch=16, col=ifelse( hs$classfr <2, 'blue','red'),xaxt="n") 
text(30,0.9,"FD distribution", cex=1.2)


points(hs$absLR,c3, pch=16, col=ifelse( hs$classabs <2, 'blue','red'),xaxt="n") 
text(30,0.4,"|LR| distribution", cex=1.2)

points(hs$LOG,c4, pch=16, col=ifelse( hs$class <2, 'blue','red'),xaxt="n") 
text(30,-0.6,"LOG distribution", cex=1.2)


points(hs$LR,c5, pch=16, col=ifelse( hs$classlr <2, 'blue','red'),ylab='',xaxt="n") 
text(30,-1,"LR distribution", cex=1.2)

dev.off()


pdf(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumFeature.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis-dec/Validation-Nov2015/Dec2-2015-1dPlots/sumFeature.csv', head=TRUE, sep="," )
c<-rep(0,280)
c2<-rep(1,280)
c3<-rep(0.5,280)
c4<-rep(-0.5,280)
c5<-rep(-0.9,280)

plot(hs$ELD-8,c, pch=16, col=ifelse(  hs$class<2, 'blue','red'),main="Single Feature Synthetic dataset", xlab='Metrics',ylab='',yaxt="n",cex.lab=1.3) 
text(30,-0.1,"ELD distribution", cex=1.2)


points(hs$FD*4,c2, pch=16, col=ifelse( hs$classfd <2, 'blue','red'),xaxt="n") 
text(30,0.9,"FD distribution", cex=1.2)


points(hs$absLR,c3, pch=16, col=ifelse( hs$class <2, 'blue','red'),xaxt="n") 
text(30,0.4,"|LR| distribution", cex=1.2)

points(hs$LOG,c4, pch=16, col=ifelse( hs$classLOG >1, 'blue','red'),xaxt="n") 
text(30,-0.6,"LOG distribution", cex=1.2)


points(hs$LR,c5, pch=16, col=ifelse( hs$classLR <2, 'blue','red'),ylab='',xaxt="n") 
text(30,-1,"LR distribution", cex=1.2)

dev.off()
