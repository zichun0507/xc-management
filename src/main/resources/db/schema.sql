CREATE TABLE IF NOT EXISTS building (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL COMMENT '楼栋名称',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    landlord    VARCHAR(100) DEFAULT NULL COMMENT '房东名',
    created_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼栋';

CREATE TABLE IF NOT EXISTS floor (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT       NOT NULL COMMENT 'FK building.id',
    name        VARCHAR(50)  NOT NULL COMMENT '楼层名称',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    created_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_floor_building FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_floor_building (building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼层';

CREATE TABLE IF NOT EXISTS room (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT       NOT NULL COMMENT 'FK building.id',
    floor_id    BIGINT       NOT NULL COMMENT 'FK floor.id',
    room_number VARCHAR(30)  NOT NULL COMMENT '房间号',
    status      VARCHAR(20)  NOT NULL DEFAULT 'FREE' COMMENT 'FREE-空闲 OCCUPIED-已分配 DISABLED-禁用',
    created_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_room_floor    FOREIGN KEY (floor_id)    REFERENCES floor(id)    ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE KEY uk_building_room (building_id, room_number) COMMENT '同一楼栋内房间号唯一',
    INDEX idx_room_building_floor (building_id, floor_id),
    INDEX idx_room_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间';

CREATE TABLE IF NOT EXISTS company (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    company_name     VARCHAR(200) NOT NULL COMMENT '企业全称',
    short_name       VARCHAR(100) DEFAULT NULL COMMENT '简称',
    english_name     VARCHAR(100) DEFAULT NULL COMMENT '英文缩写',
    unified_code     VARCHAR(50)  DEFAULT NULL COMMENT '统一社会信用代码',
    legal_person     VARCHAR(50)  DEFAULT NULL COMMENT '法定代表人',
    contact_person   VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    contact_phone    VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    address          VARCHAR(300) DEFAULT NULL COMMENT '经营地址',
    business_status  VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL-正常运营 MOVED_OUT-迁出 SUSPENDED-停办',
    lease_start_date DATE         DEFAULT NULL COMMENT '入驻开始时间',
    lease_end_date   DATE         DEFAULT NULL COMMENT '入驻到期时间',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_company_name (company_name),
    INDEX idx_company_status (business_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入驻企业';

CREATE TABLE IF NOT EXISTS company_room (
    id             BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    company_id     BIGINT   NOT NULL COMMENT 'FK company.id',
    room_id        BIGINT   NOT NULL COMMENT 'FK room.id',
    allocated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
    released_time  DATETIME DEFAULT NULL COMMENT '释放时间（迁出时记录）',
    created_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cr_company FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE CASCADE  ON UPDATE CASCADE,
    CONSTRAINT fk_cr_room    FOREIGN KEY (room_id)    REFERENCES room(id)    ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE KEY uk_company_room (company_id, room_id) COMMENT '同企业不重复分配同一房间',
    INDEX idx_cr_company (company_id),
    INDEX idx_cr_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业-房间分配关系';

CREATE TABLE IF NOT EXISTS employee (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    company_id  BIGINT       NOT NULL COMMENT 'FK company.id',
    name        VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender      VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    id_card     VARCHAR(20)  DEFAULT NULL COMMENT '身份证号',
    school      VARCHAR(100) DEFAULT NULL COMMENT '毕业学校',
    major       VARCHAR(100) DEFAULT NULL COMMENT '专业',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    photo       VARCHAR(500) DEFAULT NULL COMMENT '照片存储路径',
    position    VARCHAR(100) DEFAULT NULL COMMENT '岗位',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE-在职 INACTIVE-离职',
    created_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_company FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_employee_company (company_id),
    INDEX idx_employee_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业人员';

CREATE TABLE IF NOT EXISTS attachment (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id     BIGINT       NOT NULL COMMENT 'FK employee.id',
    attachment_type VARCHAR(30)  NOT NULL COMMENT 'ID_CARD_FRONT/ID_CARD_BACK/GRADUATION_CERT/EDUCATION_REPORT',
    original_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_path     VARCHAR(500) NOT NULL COMMENT '服务器存储路径',
    file_size       BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    file_format     VARCHAR(20)  NOT NULL COMMENT '文件格式扩展名',
    uploaded_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachment_employee FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_attachment_employee (employee_id),
    INDEX idx_attachment_type (attachment_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员附件';

CREATE TABLE IF NOT EXISTS doc_template (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    template_name   VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_type   VARCHAR(30)  DEFAULT NULL COMMENT '模板分类',
    stored_path     VARCHAR(500) NOT NULL COMMENT '服务器存储路径',
    original_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_size       BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    file_format     VARCHAR(20)  NOT NULL COMMENT '文件格式',
    uploaded_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代办模板';

CREATE TABLE IF NOT EXISTS operation_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    operator_id     BIGINT        NOT NULL COMMENT '操作人ID sys_user.id',
    operator_name   VARCHAR(50)   NOT NULL COMMENT '操作人姓名',
    module          VARCHAR(30)   NOT NULL COMMENT '业务模块 ROOM/COMPANY/EMPLOYEE',
    action          VARCHAR(30)   NOT NULL COMMENT '操作动作 CREATE/UPDATE/DELETE/STATUS_CHANGE',
    content         VARCHAR(2000) NOT NULL COMMENT '操作内容描述',
    operation_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_ol_operator (operator_id),
    INDEX idx_ol_module_time (module, operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(200) NOT NULL COMMENT 'BCrypt密文',
    real_name   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'ADMIN-管理员 USER-普通用户',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED-启用 DISABLED-禁用',
    created_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';