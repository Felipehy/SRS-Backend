ALTER TABLE tbl_auditorium
ALTER COLUMN date_create TYPE timestamptz(6)
        USING date_create AT TIME ZONE 'UTC',
    ALTER COLUMN date_analysis TYPE timestamptz(6)
        USING date_analysis AT TIME ZONE 'UTC',
    ALTER COLUMN date_cancellation TYPE timestamptz(6)
        USING date_cancellation AT TIME ZONE 'UTC';

ALTER TABLE tbl_meeting_room
ALTER COLUMN date_create TYPE timestamptz(6)
        USING date_create AT TIME ZONE 'UTC',
    ALTER COLUMN date_analysis TYPE timestamptz(6)
        USING date_analysis AT TIME ZONE 'UTC',
    ALTER COLUMN date_cancellation TYPE timestamptz(6)
        USING date_cancellation AT TIME ZONE 'UTC';

ALTER TABLE tbl_fleet_vehicles
ALTER COLUMN date_create TYPE timestamptz(6)
        USING date_create AT TIME ZONE 'UTC',
    ALTER COLUMN date_analysis TYPE timestamptz(6)
        USING date_analysis AT TIME ZONE 'UTC',
    ALTER COLUMN date_cancellation TYPE timestamptz(6)
        USING date_cancellation AT TIME ZONE 'UTC';

ALTER TABLE tbl_logs
ALTER COLUMN modification_date TYPE timestamptz(6)
        USING modification_date AT TIME ZONE 'UTC';