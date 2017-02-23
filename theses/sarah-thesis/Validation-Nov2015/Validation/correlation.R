pdf(file='C:/Users/Fatemeh/Downloads/Validation-Nov2015/Validation/Bernoulli-SV-ELDvsFD.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Downloads/Validation-Nov2015/Validation/Bernoulli-SV-ELDvsFD.csv', head=FALSE, sep="," )
plot(hs$V1,hs$V2, pch=16, col=ifelse( hs$V3 <2, 'blue','red'), xlab='ELD',ylab='FD') 
#abline(h = 0, v = 0, col = "gray60")
dev.off()



pdf(file='C:/Users/atemeh/Documents/thesis/Validation-Nov2015/Validation/Bernoulli-Sep-ELDvsFD.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Downloads/Validation-Nov2015/Validation/Bernoulli-Sep-ELDvsFD.csv', head=FALSE, sep="," )
plot(hs$V1,hs$V2, pch=16, col=ifelse( hs$V3 <2, 'blue','red'), xlab='ELD',ylab='FD') 
#abline(h = 0, v = 0, col = "gray60")
dev.off()


pdf(file='C:/Users/Fatemeh/Downloads/Validation-Nov2015/Validation/StrikersELDvsFD.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Downloads/Validation-Nov2015/Validation/StrikersELDvsFD.csv', head=FALSE, sep="," )
plot(hs$V1,hs$V2, pch=16, col=ifelse( hs$V3 <3, 'blue','red'), xlab='ELD',ylab='FD') 
#abline(h = 0, v = 0, col = "gray60")
dev.off()



pdf(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/ELDPrior-Sep.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/ELDPrior-Sep.csv', head=FALSE, sep="," )
plot(hs$V2,hs$V3, pch=16, col=ifelse( hs$V1 <2, 'blue','red'), xlab='ELD',ylab='FD') 
#abline(h = 0, v = 0, col = "gray60")
dev.off()

pdf(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/1D-ELDPrior-Sep.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/ELDPrior-Sep.csv', head=FALSE, sep="," )
c<-seq(1,280,1)
plot(c,hs$V2, pch=16, col=ifelse( hs$V1 <2, 'blue','red'), xlab='points',ylab='ELD') 
#abline(h = 0, v = 0, col = "gray60")
dev.off()



pdf(file='C:/Users/Fatemeh/Downloads/Validation-Nov2015/Validation/ELDPrior-SV.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Downloads/Validation-Nov2015/Validation/ELDPrior-SV.csv', head=FALSE, sep="," )
plot(hs$V2,hs$V3, pch=16, col=ifelse( hs$V1 <2, 'blue','red'), xlab='ELD',ylab='FD') 
#abline(h = 0, v = 0, col = "gray60")
dev.off()

pdf(file='C:/Users/Fatemeh/Downloads/Validation-Nov2015/Validation/1-DELDPrior-SV.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Downloads/Validation-Nov2015/Validation/ELDPrior-SV.csv', head=FALSE, sep="," )
c<-seq(1,280,1)
plot(c,hs$V2, pch=16, col=ifelse( hs$V1 <2, 'blue','red'), xlab='points',ylab='ELD') 
#abline(h = 0, v = 0, col = "gray60")
dev.off()


pdf(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/ELDPrior-Strikers.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/ELDPrior-Strikers.csv', head=FALSE, sep="," )
plot(hs$V2,hs$V3, pch=16, col=ifelse( hs$V1 <3, 'red','blue'), xlab='ELD',ylab='FD') 
#abline(h = 0, v = 0, col = "gray60")
dev.off()

pdf(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/1-DELDPrior-Strikers.pdf') 
hs <- read.csv(file='C:/Users/Fatemeh/Documents/thesis/Validation-Nov2015/Validation/ELDPrior-Strikers.csv', head=FALSE, sep="," )
c<-seq(1,171,1)
plot(c,hs$V2, pch=16, col=ifelse( hs$V1 <3, 'red','blue'), xlab='Points',ylab='ELD') 
#abline(h = 0, v = 0, col = "gray60")
dev.off()
par(new=T)

 plot(hs$V2*10,c, pch=16, col=ifelse( hs$V3 <2, 'blue','red'), xlab='ELD',ylab=' ',ylim=c(-1,3),xlim(0,30) )

 points(hs$V1,c1, pch=16, col=ifelse( hs$V3 <2, 'blue','red'), xlab='ELD',ylab=' ')

