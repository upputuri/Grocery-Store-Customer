package com.dolittle.ecom.customer.bo;

import java.util.Date;

public class User {
    private int uid;
    private String name;
    private String userId;
    private String email;
    private String password;
    private String typeAuser;
    private int ustatusid;
    private Date createdTs;
    private Date updatedTs;

    public User(int uid, String userId, String name)
    {
        this.uid = uid;
        this.name = name;
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTypeAuser() {
        return typeAuser;
    }

    public void setTypeAuser(String typeAuser) {
        this.typeAuser = typeAuser;
    }

    public int getUstatusid() {
        return ustatusid;
    }

    public void setUstatusid(int ustatusid) {
        this.ustatusid = ustatusid;
    }

    public Date getCreatedTs() {
        return createdTs;
    }

    public void setCreatedTs(Date createdTs) {
        this.createdTs = createdTs;
    }

    public Date getUpdatedTs() {
        return updatedTs;
    }

    public void setUpdatedTs(Date updatedTs) {
        this.updatedTs = updatedTs;
    }

    @Override
    public String toString() {
        return this.uid + " " + this.name + " " + this.userId;
    }

    public int getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }
}
