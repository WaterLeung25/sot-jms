package jms.user;

import com.owlike.genson.Genson;
import jms.admin.User;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Properties;

public class GameUser {
    private JPanel panel1;
    private JTextField messageTextField;
    private JButton sendButton;
    private JList list1;
    private JTextField userIdTextField;
    private JButton checkScoreButton;

    private Connection connection;
    private Session session;
    private Destination sendDestination;
    private Destination receiveDestination;
    private MessageProducer producer;
    private MessageConsumer consumer;
    private Message requestMsg;
    private String send;

    private String replyQueue = "replyQ2"; //start a new client, change it to another queue

    DefaultListModel<String> model = new DefaultListModel<>();

    Hashtable<String, Integer> mapForReceivingMessage = new Hashtable<>();
    Hashtable<String, Integer> mapForReceivingMessagePosition = new Hashtable<>();

    public GameUser() {
        list1.setModel(model);

        checkScoreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = userIdTextField.getText();
                if (id.equals("")){
                    JOptionPane.showMessageDialog(null, "Please input your user id.");
                    return;
                }
                int userId;
                try {
                    userId = Integer.parseInt(id);
                } catch (NumberFormatException e1){
                    JOptionPane.showMessageDialog(null, "Only numbers are allowed to input as a user id.");
                    return;
                }
                requestScore(userId);
                System.out.println("User(" + id + ") is requesting score.\n");
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = userIdTextField.getText();
                String msg = messageTextField.getText();

                if (id.equals("")){
                    JOptionPane.showMessageDialog(null, "Please input your user id.");
                    return;
                }
                int userId;
                try {
                    userId = Integer.parseInt(id);
                } catch (NumberFormatException e1){
                    JOptionPane.showMessageDialog(null, "Only numbers are allowed to input as a user id.");
                    return;
                }

                if (msg.equals("")){
                    JOptionPane.showMessageDialog(null, "Your message cannot be empty.");
                    return;
                }

                sendMessage(userId, msg);
                System.out.println("User(" + id + ") is sending a message");
            }
        });
    }

    public void connect(){
        try {
            Properties properties = new Properties();
            properties.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            properties.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");
            properties.put(("queue.requestDestination"), "requestDestination");
            properties.put(("queue." + replyQueue), replyQueue);
            Context jndiContext = new InitialContext(properties);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");

            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            sendDestination = (Destination) jndiContext.lookup("requestDestination");
            receiveDestination = (Destination) jndiContext.lookup(replyQueue);
            producer = session.createProducer(sendDestination);
            consumer = session.createConsumer(receiveDestination);

            connection.start();

            getRequest();
        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }
    }

    public void requestScore(int id){
        try {
            model.addElement("User(" + id + ") is waiting his/her information......");
            requestMsg = session.createTextMessage(Integer.toString(id) + "# ");

            requestMsg.setJMSReplyTo(receiveDestination);
            producer.send(requestMsg);

            String messageId = requestMsg.getJMSMessageID();
            mapForReceivingMessage.put(messageId, id);
            mapForReceivingMessagePosition.put(messageId, model.size() -1);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void getRequest(){
        try {
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        String correlationId = message.getJMSCorrelationID();
                        if (correlationId != null) {
                            int userId = mapForReceivingMessage.get(correlationId);
                            int index = mapForReceivingMessagePosition.get(correlationId);
                            TextMessage textMessage = (TextMessage) message;
                            String text = textMessage.getText();
                            if (text.contains("#")){
                                String[] elements = text.split("#");
                                String msg = elements[0];
                                model.setElementAt(send + " >>Administrator reply user(" + userId + "): " + msg, index);
                                return;
                            }
                            Genson genson = new Genson();
                            User user = genson.deserialize(text, User.class);

                            if (user == null){
                                model.setElementAt("User(" + userId + ") have not play any game yet.", index);
                            } else {
                                model.setElementAt("User(" + userId + ") current score is " + user.getScore(), index);
                            }
                        }
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                    System.out.println("received: " + message + "\n");
                }
            });
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(int id, String message){
        try {
            send = "User(" + id + "): " + message;
            model.addElement(send);
            requestMsg = session.createTextMessage(Integer.toString(id) + "#" + message + "#");

            requestMsg.setJMSReplyTo(receiveDestination);
            producer.send(requestMsg);

            String messageId = requestMsg.getJMSMessageID();
            mapForReceivingMessage.put(messageId, id);
            mapForReceivingMessagePosition.put(messageId, model.size() -1);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] arg){
        GameUser gameUser = new GameUser();
        gameUser.connect();
        JFrame frame = new JFrame("User Request");
        frame.setContentPane(gameUser.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(600, 400));
        frame.pack();
        frame.setVisible(true);

    }

}
