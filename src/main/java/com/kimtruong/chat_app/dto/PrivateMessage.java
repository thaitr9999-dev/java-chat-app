package com.kimtruong.chat_app.dto;

public class PrivateMessage {
    private String sender ; 
    private String recipient ; 
    private String content ; 

    public PrivateMessage(){}

    public String getSender (){
        return sender; 
    }
    public String getRecipient(){return recipient;}
    public String getContent(){return content;}

    public void setSender(String sender) { this.sender = sender; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public void setContent(String content) { this.content = content; }


    
}
