package etcd_code

import (
	"context"
	"fmt"
	v3 "go.etcd.io/etcd/clientv3"
	"testing"
	"time"
)

func TestLeaseGrant(t *testing.T) {
	cli := newClient(t)
	defer cli.Close()
	key := "TestLeaseGrant"

	ctx := context.Background()

	// Grant a lease
	resp, err := cli.Grant(ctx, 10)

	// Put a key with a lease
	_, err = cli.Put(ctx, key, "1", v3.WithLease(resp.ID))
	if err != nil {
		t.Fatal(err)
	}
	// The key will be removed after 5 seconds. Use etcdctl check it.
	time.Sleep(15 * time.Second)
	fmt.Println("woke up")
}

func TestLeaseRevoke(t *testing.T) {
	cli := newClient(t)
	defer cli.Close()
	key := "TestLeaseRevoke"
	ctx := context.Background()

	resp, err := cli.Grant(ctx, 5)
	if err != nil {
		t.Fatal(err)
	}
	_, err = cli.Put(ctx, key, "1", v3.WithLease(resp.ID))
	if err != nil {
		t.Fatal(err)
	}

	// Revoking lease expires the key attached to the lease ID
	_, err = cli.Revoke(ctx, resp.ID)
	if err != nil {
		t.Fatal(err)
	}

	gresp, err := cli.Get(ctx, key)
	if err != nil {
		t.Fatal(err)
	}
	fmt.Println("number of keys:", len(gresp.Kvs))
	if keyCount := len(gresp.Kvs); keyCount != 0 {
		t.Errorf("expected %d keys, but got %d keys", 0, keyCount)
	}
	// Output: number of keys: 0
}

func TestLeaseWithKeepAlive(t *testing.T) {
	cli := newClient(t)
	defer cli.Close()
	key := "TestLeaseWithKeepAlive"
	ctx := context.Background()

	resp, err := cli.Grant(ctx, 5)
	if err != nil {
		t.Fatal(err)
	}
	_, err = cli.Put(context.TODO(), key, "1", v3.WithLease(resp.ID))
	if err != nil {
		t.Fatal(err)
	}

	// Since the lease will be kept alive, the
	ch, kaerr := cli.KeepAlive(context.TODO(), resp.ID)
	if kaerr != nil {
		t.Fatal(kaerr)
	}

	ka := <-ch
	fmt.Println("ttl:", ka.TTL)
	// Output: ttl: 5
	seconds := 50
	fmt.Printf("sleeping for %d seconds\n", seconds)
	// During this time, "etcdctl get TestLeaseWithKeepAlive" always return a value
	time.Sleep(20 * time.Second)
	fmt.Printf("woke up, the key should be deleted after some time\n")
}

func TestLeaseWithKeepAliveOnce(t *testing.T) {
	cli := newClient(t)
	defer cli.Close()
	key := "TestLeaseWithKeepAliveOnce"
	ctx := context.Background()

	// Put with Lease
	resp, err := cli.Grant(ctx, 5)
	if err != nil {
		t.Fatal(err)
	}
	_, err = cli.Put(ctx, key, "bar", v3.WithLease(resp.ID))
	if err != nil {
		t.Fatal(err)
	}

	go func() {
		time.Sleep(8 * time.Second)
		response, err := cli.Get(ctx, key)
		if err != nil {
			t.Fatal(err)
		}
		fmt.Printf("kvs: %q\n", response.Kvs)
		if response.Count != 1 {
			t.Fatalf("expected count %d, but got %d\n", 1, response.Count)
		}
	}()

	// Renew the lease only once. If the following code block is commented,
	// the above response.Count check will fail.
	time.Sleep(4 * time.Second)
	_, kaerr := cli.KeepAliveOnce(ctx, resp.ID)
	if kaerr != nil {
		t.Fatal(kaerr)
	}

	time.Sleep(12 * time.Second)
}
