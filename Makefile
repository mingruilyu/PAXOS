GS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        LogEntry.java \
        Log.java \
        Dispatcher.java \
        NewMessageTask.java \
        Server.java \
        Message.java \
        Operation.java \
        ServerTimer.java \
        Messenger.java \
        Terminal.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
