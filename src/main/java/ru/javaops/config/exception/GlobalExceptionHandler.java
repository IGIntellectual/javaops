package ru.javaops.config.exception;

import com.google.api.client.repackaged.com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.thymeleaf.exceptions.TemplateInputException;
import ru.javaops.AuthorizedUser;
import ru.javaops.util.exception.NoPartnerException;
import ru.javaops.util.exception.PaymentException;

import javax.servlet.http.HttpServletRequest;

/**
 * User: gkislin
 * Date: 23.09.2014
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoPartnerException.class)
    public ModelAndView noPartnerException(HttpServletRequest req, NoPartnerException pe) throws Exception {
        log.error("No partner request " + req.getRequestURL() + " from " + pe.getPartnerKey());
        return new ModelAndView("message/noRegisteredPartner", "email", pe.getPartnerKey());
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity paymentException(HttpServletRequest req, PaymentException e) throws Exception {
        log.error("!!! PaymentException: {}, request {}, params {}", e.getMessage(), req.getRequestURL(), e.getRequestParams());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @ExceptionHandler(TemplateInputException.class)
    public ModelAndView templateInputException(HttpServletRequest req, TemplateInputException e) throws Exception {
        return processException(req, e.getMessage(), null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ModelAndView missingServletRequestParameterException(HttpServletRequest req, MissingServletRequestParameterException e) throws Exception {
        return processException(req, e.getMessage(), null);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ModelAndView noHandlerFoundHandler(HttpServletRequest req, NoHandlerFoundException e) throws Exception {
        return processException(req, "Неверный запрос", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView illegalArgumentHandler(HttpServletRequest req, Exception e) throws Exception {
        return processException(req, e.getMessage(), null);
    }

/*
    @ExceptionHandler(ValidationException.class)
    public ModelAndView bindValidationError(HttpServletRequest req, BindingResult result) {
        log.error("Illegal binding in request " + req.getRequestURL());
        StringBuilder sb = new StringBuilder();
        result.getFieldErrors().forEach(fe -> sb.append(Strings.isNullOrEmpty(fe.getField()) ? "Поле формы" : fe.getField()).append(" ").append(fe.getDefaultMessage()).append("<br>"));
        return processException(sb.toString());
    }
*/

    @ExceptionHandler(Throwable.class)
    public ModelAndView defaultHandler(HttpServletRequest req, Throwable e) throws Exception {
        return processException(req, null, e);
    }

    private ModelAndView processException(HttpServletRequest req, String msg, Throwable e) {
        if (e == null) {
            log.error("{}: {}", req.getRequestURL(), msg);
        } else {
            e = Throwables.getRootCause(e);
            msg = e.getMessage();
            log.error(req.getRequestURL() + ": " + msg, e);
        }
        ModelAndView modelAndView = new ModelAndView("exception", "message", msg);
        modelAndView.getModelMap().addAttribute("authUser", AuthorizedUser.user());
        return modelAndView;
    }
}