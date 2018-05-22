package jms.admin;

import com.owlike.genson.Genson;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

public class GameAdmin {
    private JPanel panel1;
    private JList list1;
    private JTextField textField1;
    private JButton replyScoreButton;
    private JButton replyMessageButton;

    private Connection connection;
    private Session session;
    private Destination receiveDestination;
    private Destination responseDestinaton;
    private MessageProducer producer;
    private MessageConsumer consumer;

    Message replyMsg;

    DefaultListModel<String> model = new DefaultListModel<>();

    Hashtable<String, Message> mapForReceivingMessage = new Hashtable<>();
    Hashtable<String, Integer> mapForReceivingMessagePosition = new Hashtable<>();

    private List<User> users = new ArrayList<>();

    public GameAdmin() {
        list1.setModel(model);

        users.add(new User(1001, 50));
        users.add(new User(1002, 60));
        users.add(new User(1003, 70));
        users.add(new User(1004, 20));
        users.add(new User(1005, 10));
        users.add(new User(1006, 80));
        users.add(new User(1007, 90));
        users.add(new User(1008, 0));
        users.add(new User(1009, 99));
        users.add(new User(1010, 100));

        replyScoreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                replyScore();

            }
        });

        replyMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = textField1.getText();
                if (msg.equals("")){
                    JOptionPane.showMessageDialog(null,"Cannot reply empty message.");
                    return;
                }
                replyMessage(msg);
            }
        });
    }

    private User getUserById(int id){
        for (User user : users){
            if (user.getUserId() == id){
                return user;
            }
        }
        return null;
    }

    public void replyMessage(String message){
        try {
            if (list1.getSelectedValue() != null){
                String selectedValue = (String) list1.getSelectedValue();

                if (!selectedValue.contains("#")) {return;}

                String[] elements = selectedValue.split("#");
                String jmsMessageId = elements[0];
                int userId = Integer.parseInt(elements[2]);

                model.setElementAt("Replied user(" + userId + "): " + message, mapForReceivingMessagePosition.get(jmsMessageId));

                replyMsg = session.createTextMessage(message + "# ");

                String correlationId = mapForReceivingMessage.get(jmsMessageId).getJMSMessageID();
                replyMsg.setJMSCorrelationID(correlationId);
                Destination replyingDestination = mapForReceivingMessage.get(jmsMessageId).getJMSReplyTo();
                producer.send(replyingDestination, replyMsg);

            } else {
                System.err.println("No Item selected.\n");
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void replyScore(){
        try {
            if (list1.getSelectedValue() != null){
                String selectedValue = (String) list1.getSelectedValue();

                if (!selectedValue.contains("#")) {return;}

                String[] elements = selectedValue.split("#");
                String jmsMessageId = elements[0];

                int userId = Integer.parseInt(elements[2]);

                Genson genson = new Genson();
                String userScore = genson.serialize(getUserById(userId));

                replyMsg = session.createTextMessage(userScore);

                String correlationId = mapForReceivingMessage.get(jmsMessageId).getJMSMessageID();
                replyMsg.setJMSCorrelationID(correlationId);
                Destination replyingDestination = mapForReceivingMessage.get(jmsMessageId).getJMSReplyTo();
                producer.send(replyingDestination, replyMsg);

                User user = getUserById(userId);
                String replyScore = "(" + userId + ") does not have score yet.";
                if (user != null){
                    replyScore = "(" + userId + ") with score " + user.getScore();
                }
                model.setElementAt("User" + replyScore, mapForReceivingMessagePosition.get(jmsMessageId));
            } else {
                System.err.println("No Item selected.\n");
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void getRequest(){
        try {
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    if (message != null){
                        try {
                            TextMessage textMessage = (TextMessage) message;
                            String id = message.getJMSMessageID();
                            String text = textMessage.getText();
                            String[] elements = text.split("#");
                            int userId = Integer.parseInt(elements[0]);
                            if (!elements[1].equals(" ")){
                                String msg = elements[1];
                                model.addElement(message.getJMSMessageID() + "# User with id: #" + userId + "# Message: #" + msg + "#");
                            } else {
                                model.addElement(message.getJMSMessageID() + "# user with id: #" + userId + "#");
                            }


                            if (id != null){
                                mapForReceivingMessage.put(message.getJMSMessageID(), message);
                                mapForReceivingMessagePosition.put(message.getJMSMessageID(), model.size()-1);
                            }
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }


    public void connect(){
        try {
            Properties properties = new Properties();
            properties.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            properties.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");
            properties.put(("queue.requestDestination"), "requestDestination");
            properties.put(("queue.replyQ1"), "replyQ1");
            Context jndiContext = new InitialContext(properties);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");

            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            receiveDestination = (Destination) jndiContext.lookup("requestDestination");
            consumer = session.createConsumer(receiveDestination);

            producer = session.createProducer(null);

            connection.start();

            getRequest();
            //getMessage();

        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] arg){
        GameAdmin gameAdmin = new GameAdmin();
        gameAdmin.connect();
        JFrame frame = new JFrame("Admin Reply");
        frame.setContentPane(gameAdmin.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(600, 400));
        frame.pack();
        frame.setVisible(true);

    }
}
