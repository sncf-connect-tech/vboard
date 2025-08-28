/*
 * This file is part of the vboard distribution.
 * (https://github.com/sncf-connect-tech/vboard)
 * Copyright (c) 2017 VSCT.
 *
 * vboard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, version 3.
 *
 * vboard is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.vsct.vboard.services;

import com.vsct.vboard.config.EmailsConfig;
import com.vsct.vboard.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class EmailSenderService {

    private final EmailsConfig emailsConfig;

    @Autowired
    public EmailSenderService(EmailsConfig emailsConfig) {
        this.emailsConfig = emailsConfig;
    }

    public MimeMessage setEmail(User to) throws Exception{
        String host = emailsConfig.getHostname();
        String from = emailsConfig.getSender();
        String port = emailsConfig.getPort();

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.auth", "false");
        properties.setProperty("mail.smtp.starttls.enable", "false");
        properties.setProperty("mail.smtp.port", port);

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties);

        // Create a default MimeMessage object.
        MimeMessage message = new MimeMessage(session);

        // Set the RFC 822 "From" header field using the
        // value of the InternetAddress.getLocalAddress method.
        message.setFrom(new InternetAddress(from));

        // Add the given addresses to the specified recipient type.
        message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to.getEmail()));

        return message;
    }


}
