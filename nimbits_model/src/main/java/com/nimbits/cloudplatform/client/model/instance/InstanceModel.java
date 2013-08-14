/*
 * Copyright (c) 2013 Nimbits Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.  See the License for the specific language governing permissions and limitations under the License.
 */

package com.nimbits.cloudplatform.client.model.instance;

import com.nimbits.cloudplatform.client.model.common.impl.CommonFactory;
import com.nimbits.cloudplatform.client.model.email.EmailAddress;
import com.nimbits.cloudplatform.client.model.entity.Entity;
import com.nimbits.cloudplatform.client.model.entity.EntityModel;

import java.io.Serializable;
import java.util.Date;


/**
 * Created by Benjamin Sautner
 * User: BSautner
 * Date: 12/14/11
 * Time: 12:48 PM
 */
public class InstanceModel extends EntityModel implements Serializable, Instance {

    private int id;

    private String baseUrl;

    private String ownerEmail;

    private String version;

    private Date ts;

    public InstanceModel(final Entity baseEntity, final String baseUrl, final EmailAddress ownerEmail, final String serverVersion)  {
        super(baseEntity);
        this.baseUrl = baseUrl;
        this.ownerEmail = ownerEmail.getValue();
        this.version = serverVersion;
    }

    public InstanceModel(final Instance server)  {
        super(server);
        this.id = server.getId();
        this.baseUrl = server.getBaseUrl();
        this.ownerEmail = server.getOwnerEmail().getValue();
        this.version = server.getVersion();
        this.ts = server.getTs();
    }

    public InstanceModel() {
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public EmailAddress getOwnerEmail()  {
        return CommonFactory.createEmailAddress(ownerEmail);
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public  Date getTs() {
        return ts;
    }
}
