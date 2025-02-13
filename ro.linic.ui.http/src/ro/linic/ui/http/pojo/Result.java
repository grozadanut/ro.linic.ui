package ro.linic.ui.http.pojo;

import java.util.List;

import ro.linic.ui.base.services.model.GenericValue;

public record Result(List<GenericValue> resultList) {
}
