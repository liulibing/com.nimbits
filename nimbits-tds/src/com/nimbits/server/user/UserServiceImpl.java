/*
 * Copyright (c) 2010 Tonic Solutions LLC.
 *
 * http://www.nimbits.com
 *
 *
 * Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, eitherexpress or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.nimbits.server.user;

import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.*;
import com.nimbits.client.common.*;
import com.nimbits.client.enums.*;
import com.nimbits.client.exception.*;
import com.nimbits.client.model.*;
import com.nimbits.client.model.common.*;
import com.nimbits.client.model.connection.*;
import com.nimbits.client.model.email.*;
import com.nimbits.client.model.entity.*;
import com.nimbits.client.model.user.User;
import com.nimbits.client.service.user.UserService;
import com.nimbits.server.counter.*;
import com.nimbits.server.dao.counter.*;
import com.nimbits.server.email.*;
import com.nimbits.server.entity.*;
import com.nimbits.server.feed.*;

import javax.servlet.http.*;
import java.util.*;
import java.util.logging.*;


public class UserServiceImpl extends RemoteServiceServlet implements
        UserService, UserServerService {
    private static final long serialVersionUID = 1L;
    //private static final Logger log = Logger.getLogger(UserServiceImpl.class.getName());



    @Override
    public User getHttpRequestUser(final HttpServletRequest req) throws NimbitsException {


        EmailAddress email;
        User user;
        String emailParam = null;
        String secret = null;
        HttpSession session = null;
        final com.google.appengine.api.users.UserService googleUserService = UserServiceFactory.getUserService();

        if (req != null) {
            emailParam = req.getParameter(Const.Params.PARAM_EMAIL);
            secret = req.getParameter(Const.Params.PARAM_SECRET);
            session = req.getSession();
        }


        if (emailParam != null && emailParam.equals(Const.TEST_ACCOUNT)) {
            user = UserTransactionFactory.getDAOInstance().getNimbitsUser(CommonFactoryLocator.getInstance().createEmailAddress(emailParam));

        } else {

            email = (!Utils.isEmptyString(emailParam)) ?
                    CommonFactoryLocator.getInstance().createEmailAddress(emailParam) : null;

            if (email == null && session != null && (session.getAttribute(Const.Params.PARAM_EMAIL) != null)) {
                email = (EmailAddress) session.getAttribute(Const.Params.PARAM_EMAIL);
            }

            if (email == null && googleUserService.getCurrentUser() != null) {
                email = CommonFactoryLocator.getInstance().createEmailAddress(googleUserService.getCurrentUser().getEmail());
            }


            if (email != null) {


                user = UserTransactionFactory.getInstance().getNimbitsUser(email);
                if (user != null) {
                    //log.info("Found existing user");
                    // log.info("user has null email: " + String.valueOf(user.getEmail() == null));
                    // log.info(String.valueOf(user.getId()));
                    user.setRestricted(true);
                    // info.append("found user in datastore =").append(email.getValue()).append("|");
                    //did they provide a valid user secret?
                    if (!Utils.isEmptyString(secret) && !Utils.isEmptyString(user.getSecret()) && user.getSecret().equals(secret)) {
                        user.setRestricted(false);
                    }

                    //a secret was provided but it wasn't the user secret, maybe the global sever secret
//                    if (user.isRestricted() && !Utils.isEmptyString(secret)) {
//                        final String serverSecret = SettingsServiceFactory.getInstance().getServerSecret();
//                        if (!Utils.isEmptyString(serverSecret) && serverSecret.equals(secret)) {
//                            user.setRestricted(false);
//                        }
//                    }
                    //can we authenticate them with google auth?
                    //log.info("Is user authenticated with google account?" + (googleUserService.getCurrentUser() != null));
                    if (user.isRestricted()) {

                        if (googleUserService.getCurrentUser() != null
                                && googleUserService.getCurrentUser().getEmail().equalsIgnoreCase(user.getEmail().getValue())) {
                            user.setRestricted(false);
                        }
                    }
                } else {
                    if (googleUserService.getCurrentUser() != null && googleUserService.getCurrentUser().getEmail().equalsIgnoreCase(email.getValue())) {
                        user = UserTransactionFactory.getInstance().createNimbitsUser(email);
                    } else if (googleUserService.getCurrentUser() != null && !googleUserService.getCurrentUser().getEmail().equalsIgnoreCase(email.getValue())) {
                        throw new NimbitsException("While the current user is authenticated, the email provided does not match " +
                                "the authenticated user, so the system is confused and cannot authenticate the request. " +
                                "Please report this error.");
                    } else if (googleUserService.getCurrentUser() == null) {
                        throw new NimbitsException(email.getValue() + " was provided but could not be found. Please log into nimbits with an account " +
                                " registered with google at least once.");
                    }


                }
            } else {
                throw new NimbitsException("There was no account connected to this request which requires authentication");
            }
        }

        if (user == null) {

            throw new NimbitsException("The user identity could not be found in the session, database, or via google authentication." +
                    " Please report this error to support@nimbits.com");
        }

        return user;

    }


    @Override
    public User getAppUserUsingGoogleAuth() throws NimbitsException {
        com.google.appengine.api.users.UserService u = UserServiceFactory.getUserService();
        //u.getCurrentUser().

        User retObj = null;
        if (u.getCurrentUser() != null) {
            EmailAddress emailAddress = CommonFactoryLocator.getInstance().createEmailAddress(u.getCurrentUser().getEmail());
            retObj = UserTransactionFactory.getInstance().getNimbitsUser(emailAddress);
        }

        return retObj;
    }

    @Override
    public String getSecret() throws NimbitsException {

        final String email = UserServiceFactory.getUserService().getCurrentUser().getEmail().toLowerCase();
        final EmailAddress internetAddress = CommonFactoryLocator.getInstance().createEmailAddress(email);
        User u = UserTransactionFactory.getDAOInstance().getNimbitsUser(internetAddress);
        return u.getSecret();
    }

    @Override
    public User getUserByUUID(String subscriberUUID) {
        return UserTransactionFactory.getInstance().getUserByUUID(subscriberUUID);
    }


    @Override
    public String updateSecret() throws NimbitsException {

        final String email = UserServiceFactory.getUserService().getCurrentUser().getEmail().toLowerCase();
        final EmailAddress internetAddress = CommonFactoryLocator.getInstance().createEmailAddress(email);
        final UUID secret = UUID.randomUUID();
        UserTransactionFactory.getInstance().updateSecret(internetAddress, secret);
        EmailServiceFactory.getInstance().sendEmail(internetAddress, "Your Nimbits Secret has been reset to: " + secret.toString(), "Reset Nimbits Secret");
        return secret.toString();
    }



    public void sendConnectionRequest(final EmailAddress email) throws NimbitsException {
        final User user = getAppUserUsingGoogleAuth();
        final Connection f = UserTransactionFactory.getInstance().makeConnectionRequest(user, email);


        if (f != null) {
            EmailServiceFactory.getInstance().sendEmail(email, Const.getConnectionInviteEmail(user.getEmail()));
            FeedServiceFactory.getInstance().postToFeed(user, "<p>A connection request has been emailed to " +
                    email.getValue() + ". If they approve, you will see any data object of theirs that have " +
                    "their permission set to be viewable by the public or connections</p>");


        }


    }

    @Override
    public List<Connection> getPendingConnectionRequests(final EmailAddress email) {
        return UserTransactionFactory.getInstance().getPendingConnectionRequests(email);
    }

    @Override
    public List<User> getConnectionRequests(List<String> connections) {
        return UserTransactionFactory.getInstance().getConnectionRequests(connections);
    }




    @Override
    public void connectionRequestReply(final EmailAddress targetEmail,
                                       final EmailAddress requesterEmail,
                                       final String uuid,
                                       final boolean accepted) throws NimbitsException {
        final User acceptor = getAppUserUsingGoogleAuth();

        final User requester = UserTransactionFactory.getInstance().getNimbitsUser(requesterEmail);

        Entity rConnection = EntityModelFactory.createEntity(acceptor.getName(), "", EntityType.userConnection, ProtectionLevel.onlyMe, acceptor.getUuid(), requester.getUuid(), requester.getUuid());

        Entity aConnection = EntityModelFactory.createEntity(requester.getName(), "", EntityType.userConnection, ProtectionLevel.onlyMe, requester.getUuid(), acceptor.getUuid(), acceptor.getUuid());

        EntityServiceFactory.getInstance().addUpdateEntity(acceptor, aConnection);
        EntityServiceFactory.getInstance().addUpdateEntity(requester,rConnection);

        UserTransactionFactory.getInstance().updateConnectionRequest(uuid, requester, acceptor, accepted);


    }


}
