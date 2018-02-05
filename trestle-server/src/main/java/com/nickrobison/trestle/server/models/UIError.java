package com.nickrobison.trestle.server.models;

import javax.persistence.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Created by nickrobison on 2/5/18.
 */
@Entity
@Table(name = "UI_ERRORS")
public class UIError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long Id;
    private OffsetDateTime timestamp;
    private String message;
    private String location;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "ERROR_ID", referencedColumnName = "ID")
    private List<StackFrame> stackTrace;

    public UIError() {
//        Not used
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<StackFrame> getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(List<StackFrame> stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Entity
    @Table(name = "STACK_FRAMES")
    public static class StackFrame {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "ID", unique = true, nullable = false)
        private Long id;
        @Column(name = "ERROR_ID")
        private Long errorId;
        @Column(name = "column_number")
        private Integer columnNumber;
        @Column(name = "file_name")
        private String fileName;
        @Column(name = "function_name")
        private String functionName;
        @Column(name = "line_number")
        private Integer lineNumber;
        private String source;


        public StackFrame() {

        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getErrorId() {
            return errorId;
        }

        public void setErrorId(Long errorId) {
            this.errorId = errorId;
        }

        public Integer getColumnNumber() {
            return columnNumber;
        }

        public void setColumnNumber(Integer columnNumber) {
            this.columnNumber = columnNumber;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFunctionName() {
            return functionName;
        }

        public void setFunctionName(String functionName) {
            this.functionName = functionName;
        }

        public Integer getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
