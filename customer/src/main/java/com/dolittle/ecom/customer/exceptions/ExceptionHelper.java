package com.dolittle.ecom.customer.exceptions;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;


@ControllerAdvice
public class ExceptionHelper {

    public ResponseEntity<Object> handleDataAccessException(DataAccessException e)
    {

        return null;
    }
}
