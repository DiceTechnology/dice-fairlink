
create table aurora_cluster_information (server_id text, session_id text);

create or replace function aurora_replica_status() returns setof aurora_cluster_information as $$
    select * from aurora_cluster_information;
$$ language SQL;
