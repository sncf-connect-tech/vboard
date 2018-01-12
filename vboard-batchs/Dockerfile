FROM java:8-jre

RUN mkdir -p    /var/opt/batch_sync

COPY lib        /var/opt/batch_sync/lib
COPY properties /var/opt/batch_sync/properties
COPY scripts    /var/opt/batch_sync/scripts

RUN chmod u+x   /var/opt/batch_sync/scripts/*.sh

CMD ["/var/opt/batch_sync/scripts/jdbc_sync_pins.sh"]
