package loghub.receivers;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.spi.LoggingEvent;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import loghub.Event;
import loghub.Receiver;
import loghub.configuration.Beans;

@Beans({"method", "endpoint"})
public class Log4JZMQ extends Receiver {

    private Socket log4jsocket;
    private String method = "bind";
    private String endpoint = "tcp://localhost:2120";
    private int hwm = 1000;
    private final Context context;
    public Log4JZMQ(Context context, String endpoint, Map<String, Event> eventQueue) {
        super(context, endpoint, eventQueue);
        this.context = context;
    }

    @Override
    public synchronized void start() {
        log4jsocket = context.socket(ZMQ.PULL);
        log4jsocket.setHWM(hwm);
        switch (method.toLowerCase()) {
        case "bind": log4jsocket.bind(endpoint); break;
        case "connect": log4jsocket.connect(endpoint); break;
        }

        super.start();
    }    

    @Override
    public void run() {
        try {
            while (! isInterrupted()) {
                byte[] msg;
                try {
                    // Get work piece
                    msg = log4jsocket.recv();
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(msg));
                    LoggingEvent o = (LoggingEvent)ois.readObject();
                    Event event = new Event();
                    event.type = "log4j";
                    event.put("host", "");
                    event.put("path", o.getLoggerName());
                    event.put("priority", o.getLevel());
                    event.put("logger_name", o.getLoggerName());
                    event.put("thread", o.getThreadName());
                    event.put("class", o.getLocationInformation().getClassName());
                    event.put("file", o.getLocationInformation().getFileName());
                    event.put("method", o.getLocationInformation().getMethodName());
                    event.put("line", o.getLocationInformation().getLineNumber());
                    event.put("NDC", o.getNDC());
                    if(o.getThrowableStrRep() != null) {
                        List<String> stack = new ArrayList<>();
                        for(String l: o.getThrowableStrRep()) {
                            stack.add(l.replace("\t", "    "));
                        }                
                        event.put("stack_trace", stack);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, ?> m = o.getProperties();
                    if(m.size() > 0) {
                        event.put("properties", m);                
                    }
                    Date d = new Date(o.getTimeStamp());
                    event.timestamp = d;
                    event.put("message", o.getRenderedMessage());
                    send(event);
                } catch (zmq.ZError.IOException | java.nio.channels.ClosedSelectorException | org.zeromq.ZMQException e ) {
                    // ZeroMQ throws exception
                    // when context is terminated
                    try {
                        log4jsocket.close();
                    } catch (Exception e1) {
                    }
                    break;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getHwm() {
        return hwm;
    }

    public void setHwm(int hwm) {
        this.hwm = hwm;
    }

}
