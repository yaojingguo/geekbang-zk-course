package etcd_code

import (
	v3 "go.etcd.io/etcd/clientv3"
	"go.etcd.io/etcd/clientv3/concurrency"
	"testing"
	"time"
)

var (
	endPoints = []string{"localhost:2379"}
	dialTimeout = 1 * time.Second
)

func newClient(t *testing.T) *v3.Client{
	cli, err := v3.New(v3.Config{
		Endpoints:   endPoints,
		DialTimeout: dialTimeout,
	})
	if err != nil {
		t.Fatal(err)
	}
	return cli
}

func newSession(t *testing.T) (cli *v3.Client, session *concurrency.Session) {
	cli, err := v3.New(v3.Config{
		Endpoints:   endPoints,
		DialTimeout: dialTimeout,
	})
	if err != nil {
		t.Fatal(err)
	}
	session, err = concurrency.NewSession(cli)
	if err != nil {
		t.Fatal(err)
	}
	return
}