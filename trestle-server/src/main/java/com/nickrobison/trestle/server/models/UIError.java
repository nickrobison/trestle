package com.nickrobison.trestle.server.models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Created by nickrobison on 2/5/18.
 */
@Entity
@Table(name = "UI_ERRORS")
public class UIError {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "ID", updatable = false, nullable = false)
    private UUID Id;
    private OffsetDateTime timestamp;
    private String message;
    private String location;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "ERROR_ID", referencedColumnName = "ID")
    private List<StackFrame> stackTrace;

    public UIError() {
//        Not used
    }

    public UUID getId() {
        return Id;
    }

    public void setId(UUID id) {
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
        @GeneratedValue(generator = "UUID")
        @GenericGenerator(
                name = "UUID",
                strategy = "org.hibernate.id.UUIDGenerator")
        @Column(name = "ID", updatable = false, nullable = false)
        private UUID id;
        @Column(name = "ERROR_ID")
        private UUID errorId;
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
//            Not used
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public UUID getErrorId() {
            return errorId;
        }

        public void setErrorId(UUID errorId) {
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
