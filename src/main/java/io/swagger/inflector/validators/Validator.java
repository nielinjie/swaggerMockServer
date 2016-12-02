package io.swagger.inflector.validators;

import io.swagger.models.parameters.Parameter;

import java.util.Iterator;

//NOTE 用于validate不在json中的参数值。
public interface Validator {
    void validate(Object o, Parameter parameter, Iterator<Validator> next) throws ValidationException;
}