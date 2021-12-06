JCC = javac
JAVA = java
JFLAGS = -g
REMOTE_PEER_OPTS = -cp .:jsch.jar

default: compile

compile:
	$(JCC) $(JFLAGS) config/*.java
	$(JCC) $(JFLAGS) logging/*.java
	$(JCC) $(JFLAGS) message/*.java
	$(JCC) $(JFLAGS) peer/*.java
	$(JCC) $(JFLAGS) server/*.java
	$(JCC) $(JFLAGS) $(REMOTE_PEER_OPTS) StartRemotePeers.java

start-remote-peers: StartRemotePeers.class
	$(JAVA) $(REMOTE_PEER_OPTS) StartRemotePeers

clean:
	$(RM) *.class
	$(RM) config/*.class
	$(RM) logging/*.class
	$(RM) message/*.class
	$(RM) peer/*.class
	$(RM) server/*.class
	$(RM) *.log*
