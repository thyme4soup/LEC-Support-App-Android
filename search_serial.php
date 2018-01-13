<?php
    /* get variables */
    $data = $_GET['serial'] or die;
    $dbhost = '*********';
    $dbuser = '*********';
    $dbpass = '*********';
    $db = '*************';
    // redacted
	
	/* populate connection info */
    $connectionInfo = array( "Database"=>$db, "UID"=>$dbuser, "PWD"=>$dbpass);
	
    /* database setup */
    $link = sqlsrv_connect( $dbhost, $connectionInfo );
	if( $link ) {} else {
		echo "Connection could not be established.<br />";
		die( print_r( sqlsrv_errors(), true));
	}
	
    /* set up query */
	$query = "SELECT * FROM TableNumberLookup WHERE [Table Number] = ?";
	$params = array($data);
	
	/* submit query */
    $result = sqlsrv_query($link, $query, $params);
	if( $result === false ) {
		die( print_r( sqlsrv_errors(), true));
	}
	
	/* return as json */
    header('Content-type: application/json');
    echo json_encode(sqlsrv_fetch_array($result));
?>
