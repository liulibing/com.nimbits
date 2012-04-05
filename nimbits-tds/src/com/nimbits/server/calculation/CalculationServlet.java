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

package com.nimbits.server.calculation;

import com.nimbits.client.enums.*;
import com.nimbits.client.exception.*;
import com.nimbits.client.model.calculation.*;
import com.nimbits.client.model.common.*;
import com.nimbits.client.model.entity.*;
import com.nimbits.client.model.user.*;
import com.nimbits.server.gson.*;


import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

/**
 * Created by bsautner
 * User: benjamin
 * Date: 2/18/12
 * Time: 3:52 PM
 */
public class CalculationServlet extends HttpServlet {

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);

        final String json = req.getParameter(Parameters.json.getText());
        final String nameParam = req.getParameter(Parameters.name.getText());
        final Calculation c = GsonFactory.getInstance().fromJson(json, CalculationModel.class);
        final EntityName name;
        final User u;
        try {
            u = com.nimbits.server.user.UserServiceFactory.getServerInstance().getHttpRequestUser(req);
            name = CommonFactoryLocator.getInstance().createName(nameParam, EntityType.calculation);
            if ((u != null) && (!u.isRestricted()) && (c != null)) {
                CalculationServiceFactory.getInstance().addUpdateCalculation(u, null, name, c);
            }
        } catch (NimbitsException ignored) {

        }


    }
}
