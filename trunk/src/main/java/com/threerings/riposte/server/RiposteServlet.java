package com.threerings.riposte.server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RiposteServlet extends HttpServlet
{
    @Inject public RiposteServlet (PostManager manager)
    {
        _manager = manager;
    }

    @Override protected void doGet (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        // Requests without data will sometimes come in as GET, so handle both
        doPost(req, resp);
    }

    @Override protected void doPost (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        _manager.doServiceCall(req, resp);
    }

    protected PostManager _manager;
}
